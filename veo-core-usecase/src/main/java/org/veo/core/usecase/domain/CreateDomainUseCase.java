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
package org.veo.core.usecase.domain;

import java.util.Locale;
import java.util.Optional;

import jakarta.validation.Valid;

import org.veo.core.UserAccessRights;
import org.veo.core.entity.Domain;
import org.veo.core.entity.NameAbbreviationAndDescription;
import org.veo.core.entity.Translated;
import org.veo.core.entity.exception.EntityAlreadyExistsException;
import org.veo.core.entity.transform.EntityFactory;
import org.veo.core.repository.ClientRepository;
import org.veo.core.repository.DomainRepository;
import org.veo.core.repository.DomainTemplateRepository;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CreateDomainUseCase
    implements TransactionalUseCase<CreateDomainUseCase.InputData, CreateDomainUseCase.OutputData> {

  private final EntityFactory entityFactory;
  private final DomainRepository domainRepository;
  private final DomainTemplateRepository domainTemplateRepository;
  private final ClientRepository clientRepository;

  @Override
  public CreateDomainUseCase.OutputData execute(
      InputData input, UserAccessRights userAccessRights) {
    if (!domainTemplateRepository.getDomainTemplateIds(input.name).isEmpty()) {
      throw new EntityAlreadyExistsException(
          "Templates already exist for domain name '%s'".formatted(input.name));
    }
    var client = clientRepository.getActiveById(userAccessRights.getClientId());
    if (domainRepository.nameExistsInClient(input.name, client)) {
      throw new EntityAlreadyExistsException(
          "A domain with name '%s' already exists in this client".formatted(input.name));
    }

    var domain = entityFactory.createDomain(input.name, input.authority, null);
    domain.setOwner(client);

    if (input.translations != null) {
      domain.setTranslations(input.translations);
      // TODO #4275: remove compatibility code
      Optional.ofNullable(input.translations.getTranslations().get(Locale.GERMAN))
          .ifPresent(
              germanTranslations -> {
                domain.setAbbreviation(germanTranslations.getAbbreviation());
                domain.setDescription(germanTranslations.getDescription());
              });
    }
    // TODO VEO-1812: Remove assignment because domains should not have a template version
    domain.setTemplateVersion("0.1.0");
    return new OutputData(domainRepository.save(domain));
  }

  @Override
  public boolean isReadOnly() {
    return false;
  }

  @Valid
  public record InputData(
      String name, String authority, Translated<NameAbbreviationAndDescription> translations)
      implements UseCase.InputData {}

  @Valid
  public record OutputData(@Valid Domain domain) implements UseCase.OutputData {}
}
