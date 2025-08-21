/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Alexander Koderman.
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
package org.veo.core.usecase.unit;

import java.util.UUID;

import jakarta.validation.Valid;

import org.veo.core.UserAccessRights;
import org.veo.core.entity.Client;
import org.veo.core.entity.Unit;
import org.veo.core.repository.ClientRepository;
import org.veo.core.repository.GenericElementRepository;
import org.veo.core.repository.UnitRepository;
import org.veo.core.usecase.RetryableUseCase;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.UseCase.EmptyOutput;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DeleteUnitUseCase
    implements TransactionalUseCase<DeleteUnitUseCase.InputData, EmptyOutput>, RetryableUseCase {

  private final ClientRepository clientRepository;
  private final UnitRepository unitRepository;
  private final GenericElementRepository genericElementRepository;

  @Override
  public EmptyOutput execute(InputData input, UserAccessRights userAccessRights) {
    Unit unit = unitRepository.getById(input.unitId, userAccessRights);
    userAccessRights.checkUnitDeleteAllowed();

    genericElementRepository.deleteByUnit(unit);
    unitRepository.delete(unit);
    // Reload the client since the persistence context was cleared
    Client client = clientRepository.getById(userAccessRights.clientId());
    client.decrementTotalUnits();
    return EmptyOutput.INSTANCE;
  }

  @Override
  public boolean isReadOnly() {
    return false;
  }

  @Override
  public Isolation getIsolation() {
    return Isolation.REPEATABLE_READ;
  }

  @Override
  public int getMaxAttempts() {
    return 5;
  }

  @Valid
  public record InputData(UUID unitId) implements UseCase.InputData {}
}
