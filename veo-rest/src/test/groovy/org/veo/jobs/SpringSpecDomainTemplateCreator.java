/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Jonas Jordan
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.veo.jobs;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.veo.adapter.service.domaintemplate.dto.ExportDomainTemplateDto;
import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainTemplate;
import org.veo.core.repository.DomainRepository;
import org.veo.core.repository.DomainTemplateRepository;
import org.veo.core.usecase.domain.CreateDomainFromTemplateUseCase;
import org.veo.core.usecase.domaintemplate.CreateDomainTemplateUseCase;

import lombok.RequiredArgsConstructor;

/**
 * Creates test domain templates from resource files using injected application services. File
 * contents are cached.
 */
@RequiredArgsConstructor
@Component
public class SpringSpecDomainTemplateCreator {
  private final ResourcePatternResolver resourceResolver =
      new PathMatchingResourcePatternResolver(getClass().getClassLoader());
  private final ObjectMapper objectMapper;
  private final DomainTemplateRepository domainTemplateRepository;
  private Map<UUID, ExportDomainTemplateDto> domainTemplateDtos;
  private final CreateDomainTemplateUseCase createDomainTemplateUseCase;
  private final CreateDomainFromTemplateUseCase createDomainFromTemplateUseCase;
  private final DomainRepository domainRepository;

  /**
   * Creates a new domain from given domain template ID. If the domain template does not exist, it
   * is attempted to create the domain template from the corresponding test domain template resource
   * file first.
   */
  public Domain createDomainFromTemplate(UUID templateId, Client client, boolean copyProfiles) {
    if (!domainTemplateRepository.exists(templateId)) {
      createTestTemplate(templateId);
    }
    AsSystemUser.runAsAdmin(
        () -> {
          createDomainFromTemplateUseCase.execute(
              new CreateDomainFromTemplateUseCase.InputData(
                  templateId, client.getId(), copyProfiles));
        });
    return domainRepository.findAllActiveByClient(client.getId()).stream()
        .filter(
            d ->
                Optional.ofNullable(d.getDomainTemplate())
                    .map(DomainTemplate::getId)
                    .map(templateId::equals)
                    .orElse(false))
        .sorted(Comparator.comparing(Domain::getCreatedAt).reversed())
        .findFirst()
        .orElseThrow();
  }

  /**
   * Creates domain template with given ID from the corresponding test domain template resource
   * file.
   */
  public void createTestTemplate(UUID templateId) {
    var dto = getTestTemplateDto(templateId);
    AsSystemUser.runAsContentCreator(
        () -> {
          createDomainTemplateUseCase.execute(new CreateDomainTemplateUseCase.InputData(dto));
        });
  }

  private ExportDomainTemplateDto getTestTemplateDto(UUID templateId) {
    if (domainTemplateDtos == null) {
      try {
        domainTemplateDtos =
            Arrays.stream(resourceResolver.getResources("classpath*:/testdomaintemplates/*.json"))
                .map(
                    r -> {
                      try {
                        return objectMapper.readValue(
                            r.getInputStream(), ExportDomainTemplateDto.class);
                      } catch (IOException e) {
                        throw new RuntimeException(e);
                      }
                    })
                .collect(Collectors.toMap(ExportDomainTemplateDto::getId, Function.identity()));
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    return Optional.ofNullable(domainTemplateDtos.get(templateId))
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    String.format("No test domain template found with ID %s", templateId)));
  }
}
