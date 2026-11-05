/*
 * verinice.veo
 * Copyright (C) 2026  Jonas Jordan
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
package org.veo.core.usecase.profile;

import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.veo.core.UserAccessRights;
import org.veo.core.entity.TaskConfiguration;
import org.veo.core.repository.DomainRepository;
import org.veo.core.repository.InspectionFindingRepository;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SaveTaskConfigurationUseCase
    implements TransactionalUseCase<SaveTaskConfigurationUseCase.InputData, UseCase.EmptyOutput> {

  private final DomainRepository domainRepository;
  private final InspectionFindingRepository inspectionFindingRepository;

  @Override
  public EmptyOutput execute(InputData input, UserAccessRights userAccessRights) {
    var domain = domainRepository.getActiveById(input.domainId, userAccessRights.getClientId());
    domain.setTaskConfiguration(input.taskConfiguration);
    inspectionFindingRepository.updateInspections(domain, true);
    return EmptyOutput.INSTANCE;
  }

  @Override
  public boolean isReadOnly() {
    return false;
  }

  @Valid
  public record InputData(@NotNull UUID domainId, @NotNull TaskConfiguration taskConfiguration)
      implements UseCase.InputData {}
}
