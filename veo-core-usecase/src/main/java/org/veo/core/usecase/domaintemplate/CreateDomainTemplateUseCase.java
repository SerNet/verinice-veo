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
import org.veo.core.entity.Profile;
import org.veo.core.entity.exception.EntityAlreadyExistsException;
import org.veo.core.entity.state.DomainBaseState;
import org.veo.core.repository.DomainTemplateRepository;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.base.TemplateItemValidator;
import org.veo.core.usecase.service.DomainStateMapper;

import lombok.RequiredArgsConstructor;
import lombok.Value;

/** Creates given new domain template. Will never update existing domain templates. */
@RequiredArgsConstructor
public class CreateDomainTemplateUseCase
    implements TransactionalUseCase<
        CreateDomainTemplateUseCase.InputData, CreateDomainTemplateUseCase.OutputData> {
  private final DomainStateMapper mapper;
  private final DomainTemplateRepository domainTemplateRepository;

  @Override
  public OutputData execute(InputData input) {
    var state = input.domainTemplate;
    var domainTemplate = mapper.toTemplate(state);
    validateVersion(input.domainTemplate.getTemplateVersion());
    if (domainTemplateRepository.exists(domainTemplate.getId())) {
      throw new EntityAlreadyExistsException(domainTemplate);
    }

    domainTemplate.getCatalogItems().forEach(TemplateItemValidator::validate);
    domainTemplate.getProfiles().stream()
        .flatMap((Profile profile) -> profile.getItems().stream())
        .forEach(TemplateItemValidator::validate);

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
    DomainBaseState domainTemplate;
  }

  @Valid
  @Value
  public static class OutputData implements UseCase.OutputData {
    DomainTemplate domainTemplate;
  }
}
