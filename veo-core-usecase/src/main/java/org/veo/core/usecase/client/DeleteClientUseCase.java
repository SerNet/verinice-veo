/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Jonas Jordan
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
package org.veo.core.usecase.client;

import java.util.UUID;

import org.veo.core.entity.AccountProvider;
import org.veo.core.entity.Key;
import org.veo.core.entity.specification.MissingAdminPrivilegesException;
import org.veo.core.repository.ClientRepository;
import org.veo.core.repository.UnitRepository;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.unit.DeleteUnitUseCase;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DeleteClientUseCase
    implements TransactionalUseCase<DeleteClientUseCase.InputData, UseCase.EmptyOutput> {
  private final AccountProvider accountProvider;
  private final ClientRepository clientRepository;
  private final DeleteUnitUseCase deleteUnitUseCase;
  private final UnitRepository unitRepository;

  @Override
  public EmptyOutput execute(InputData input) {
    if (!accountProvider.getCurrentUserAccount().isAdmin()) {
      throw new MissingAdminPrivilegesException();
    }
    var client = clientRepository.getById(input.clientId);
    unitRepository
        .findByClient(client)
        .forEach(
            unit ->
                deleteUnitUseCase.execute(new DeleteUnitUseCase.InputData(unit.getId(), client)));
    // Reload the client since the persistence context was cleared
    clientRepository.delete(clientRepository.getById(input.clientId));
    return EmptyOutput.INSTANCE;
  }

  @Override
  public boolean isReadOnly() {
    return false;
  }

  public record InputData(Key<UUID> clientId) implements UseCase.InputData {}
}
