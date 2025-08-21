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

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.veo.core.UserAccessRights;
import org.veo.core.entity.AccountProvider;
import org.veo.core.entity.ClientOwned;
import org.veo.core.entity.specification.MissingAdminPrivilegesException;
import org.veo.core.repository.ClientRepository;
import org.veo.core.repository.UnitRepository;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.unit.DeleteUnitUseCase;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DeleteClientUseCase
    implements TransactionalUseCase<DeleteClientUseCase.InputData, UseCase.EmptyOutput> {
  private final AccountProvider accountProvider;
  private final ClientRepository clientRepository;
  private final DeleteUnitUseCase deleteUnitUseCase;
  private final UnitRepository unitRepository;

  @Override
  public EmptyOutput execute(InputData input, UserAccessRights userAccessRights) {
    if (!accountProvider.getCurrentUserAccount().isAdmin()) {
      throw new MissingAdminPrivilegesException();
    }
    var client = clientRepository.getById(input.clientId);
    unitRepository
        .findByClient(client)
        .forEach(
            unit ->
                deleteUnitUseCase.execute(
                    new DeleteUnitUseCase.InputData(unit.getId()),
                    new NoRestriction(client.getIdAsString())));
    // Reload the client since the persistence context was cleared
    clientRepository.delete(clientRepository.getById(input.clientId));
    return EmptyOutput.INSTANCE;
  }

  @Override
  public boolean isReadOnly() {
    return false;
  }

  public record InputData(UUID clientId) implements UseCase.InputData {}

  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  private final class NoRestriction implements UserAccessRights {
    private final String clientId;

    @Override
    public void checkClient(ClientOwned id) {}

    @Override
    public boolean isUnitAccessRestricted() {
      return false;
    }

    @Override
    public Set<UUID> getReadableUnitIds() {
      return Collections.emptySet();
    }

    @Override
    public Set<UUID> getWritableUnitIds() {
      return Collections.emptySet();
    }

    @Override
    public List<String> getRoles() {
      return Collections.emptyList();
    }

    @Override
    public String getClientId() {
      return clientId;
    }

    @Override
    public String getUsername() {
      return "system";
    }
  }
}
