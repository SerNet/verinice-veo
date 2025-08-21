/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2024  Jochen Kemnade
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

import java.util.List;
import java.util.UUID;

import org.veo.core.UserAccessRights;
import org.veo.core.entity.BreakingChange;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.repository.DomainRepository;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.common.DomainDiff;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class GetBreakingChangesUseCase
    implements TransactionalUseCase<
        GetBreakingChangesUseCase.InputData, GetBreakingChangesUseCase.OutputData> {

  private final DomainRepository repository;

  @Override
  public OutputData execute(InputData input, UserAccessRights userAccessRights) {
    var domain = repository.getActiveById(input.domainId, input.authenticatedClientId);
    var template = domain.getDomainTemplate();
    if (template == null) {
      throw new NotFoundException("No domain template found for domain %s", input.domainId);
    }
    return new OutputData(DomainDiff.determineBreakingChanges(domain, template));
  }

  public record InputData(UUID authenticatedClientId, UUID domainId) implements UseCase.InputData {}

  public record OutputData(List<BreakingChange> breakingChanges) implements UseCase.OutputData {}
}
