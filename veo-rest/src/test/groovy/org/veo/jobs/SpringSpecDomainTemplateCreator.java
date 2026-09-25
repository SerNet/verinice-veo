/*
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
 */
package org.veo.jobs;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import org.veo.adapter.service.domaintemplate.dto.ExportDomainTemplateDto;
import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.core.repository.ClientRepository;
import org.veo.core.repository.DomainRepository;
import org.veo.core.repository.DomainTemplateRepository;
import org.veo.core.usecase.TemplateItems;
import org.veo.core.usecase.domaintemplate.CreateDomainTemplateUseCase;
import org.veo.core.usecase.service.DomainTemplateService;
import org.veo.rest.security.NoRestrictionAccessRight;

import groovy.lang.Closure;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.json.JsonMapper;

/**
 * Creates test domain templates from resource files using injected application services. File
 * contents are cached.
 */
@RequiredArgsConstructor
@Component
public class SpringSpecDomainTemplateCreator {
  private final ResourcePatternResolver resourceResolver =
      new PathMatchingResourcePatternResolver(getClass().getClassLoader());
  private final JsonMapper jsonMapper;
  private final DomainTemplateRepository domainTemplateRepository;
  private Map<UUID, ExportDomainTemplateDto> domainTemplateDtos;
  private final CreateDomainTemplateUseCase createDomainTemplateUseCase;
  private final DomainTemplateService domainTemplateService;
  private final DomainRepository domainRepository;
  private final ClientRepository clientRepository;

  /**
   * Creates a new domain from given domain template ID. If the domain template does not exist, it
   * is attempted to create the domain template from the corresponding test domain template resource
   * file first.
   */
  public Domain createDomainFromTemplate(
      UUID templateId, Client client, TemplateItems templateItems) {
    if (!domainTemplateRepository.exists(templateId)) {
      createTestTemplate(templateId);
    }
    return new UserSwitcher()
        .runAsAdmin(
            new Closure<>(this) {
              @Override
              public Domain call() {
                var c = clientRepository.getActiveById(client.getId());
                var domain = domainTemplateService.createDomain(c, templateId, templateItems);
                c.addToDomains(domain);
                c = clientRepository.save(c);
                return c.getDomains().stream()
                    .filter(t -> t.getId().equals(domain.getId()))
                    .findFirst()
                    .get();
              }
            });
  }

  /**
   * Creates domain template with given ID from the corresponding test domain template resource
   * file.
   */
  public void createTestTemplate(UUID templateId) {
    var dto = getTestTemplateDto(templateId);
    AsSystemUser.runAsContentCreator(
        () -> {
          createDomainTemplateUseCase.execute(
              new CreateDomainTemplateUseCase.InputData(dto), NoRestrictionAccessRight.from(null));
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
                        return jsonMapper.readValue(
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
