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
package org.veo.core.usecase.domaintemplate;

import static org.veo.core.usecase.domaintemplate.DomainTemplateValidator.validateVersion;

import jakarta.validation.Valid;

import org.veo.core.entity.DomainTemplate;
import org.veo.core.entity.Key;
import org.veo.core.entity.exception.EntityAlreadyExistsException;
import org.veo.core.repository.DomainTemplateRepository;
import org.veo.core.service.DomainTemplateIdGenerator;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.base.CatalogItemValidator;

import lombok.RequiredArgsConstructor;
import lombok.Value;

/** Creates given new domain template. Will never update existing domain templates. */
@RequiredArgsConstructor
public class CreateDomainTemplateUseCase
    implements TransactionalUseCase<
        CreateDomainTemplateUseCase.InputData, CreateDomainTemplateUseCase.OutputData> {
  private final DomainTemplateRepository domainTemplateRepository;
  private final DomainTemplateIdGenerator domainTemplateIdGenerator;

  @Override
  public OutputData execute(InputData input) {
    var domainTemplate = input.domainTemplate;

    // Generate domain template UUID (ID from input is ignored).
    domainTemplate.setId(
        Key.uuidFrom(
            domainTemplateIdGenerator.createDomainTemplateId(
                domainTemplate.getName(), domainTemplate.getTemplateVersion())));

    validateVersion(input.domainTemplate.getTemplateVersion());
    if (domainTemplateRepository.exists(domainTemplate.getId())) {
      throw new EntityAlreadyExistsException(domainTemplate);
    }

    domainTemplate.getCatalogs().stream()
        .flatMap(c -> c.getCatalogItems().stream())
        .forEach(
            item -> {
              item.getElement().setDesignator("NO_DESIGNATOR");
              CatalogItemValidator.validate(item);
            });

    domainTemplate = domainTemplateRepository.save(domainTemplate);

    return new OutputData(domainTemplate);
  }

  @Override
  public boolean isReadOnly() {
    return false;
  }

  @Valid
  @Value
  public static class InputData implements UseCase.InputData {
    DomainTemplate domainTemplate;
  }

  @Valid
  @Value
  public static class OutputData implements UseCase.OutputData {
    DomainTemplate domainTemplate;
  }
}
