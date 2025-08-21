/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Jonas Jordan
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

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.Valid;

import org.veo.core.UserAccessRights;
import org.veo.core.entity.inspection.Inspection;
import org.veo.core.repository.DomainRepository;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SaveInspectionUseCase
    implements TransactionalUseCase<
        SaveInspectionUseCase.InputData, SaveInspectionUseCase.OutputData> {

  private final DomainRepository repository;

  @Override
  public OutputData execute(InputData input, UserAccessRights userAccessRights) {
    var domain = repository.getActiveById(input.domainId, input.authenticatedClientId);
    boolean isNew = domain.applyInspection(input.inspectionId, input.inspection);
    domain.setUpdatedAt(Instant.now());
    return new OutputData(isNew);
  }

  @Override
  public boolean isReadOnly() {
    return false;
  }

  @Valid
  public record InputData(
      UUID authenticatedClientId, UUID domainId, String inspectionId, Inspection inspection)
      implements UseCase.InputData {}

  @Valid
  public record OutputData(boolean newInspection) implements UseCase.OutputData {}
}
