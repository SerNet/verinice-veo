/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2019  Alexander Koderman.
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

import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import jakarta.validation.Valid;

import org.veo.core.entity.Client;
import org.veo.core.entity.Key;
import org.veo.core.repository.ClientRepository;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;

import lombok.Value;

/** Reinstantiate a persisted client object. */
public class GetClientUseCase
    implements TransactionalUseCase<GetClientUseCase.InputData, GetClientUseCase.OutputData> {

  private final ClientRepository repository;

  public GetClientUseCase(ClientRepository repository) {
    this.repository = repository;
  }

  /**
   * Find a persisted client object and reinstantiate it. Throws a domain exception if the requested
   * client object was not found in the repository.
   */
  @Override
  @Transactional(TxType.SUPPORTS)
  public OutputData execute(InputData input) {
    return new OutputData(repository.getById(input.getClientId()));
  }

  @Valid
  @Value
  public static class InputData implements UseCase.InputData {
    Key<UUID> clientId;
  }

  @Valid
  @Value
  public static class OutputData implements UseCase.OutputData {
    @Valid Client client;
  }
}
