/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2024  Jonas Jordan
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

import java.util.UUID;

import jakarta.validation.Valid;

import org.veo.core.entity.Client;
import org.veo.core.entity.DomainTemplate;
import org.veo.core.entity.Key;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.repository.DomainTemplateRepository;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DeleteProfileInDomainTemplateUseCase
    implements TransactionalUseCase<
        DeleteProfileInDomainTemplateUseCase.InputData, UseCase.EmptyOutput> {
  private final DomainTemplateRepository domainTemplateRepository;

  @Override
  public EmptyOutput execute(InputData input) {
    var domainTemplate =
        domainTemplateRepository
            .findById(input.domainTemplateId)
            .orElseThrow(() -> new NotFoundException(input.domainTemplateId, DomainTemplate.class));
    domainTemplate.removeProfile(input.profileId);
    return EmptyOutput.INSTANCE;
  }

  @Override
  public boolean isReadOnly() {
    return false;
  }

  @Valid
  public record InputData(Client client, Key<UUID> domainTemplateId, Key<UUID> profileId)
      implements UseCase.InputData {}
}
