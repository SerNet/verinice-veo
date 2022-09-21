/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Jochen Kemnade.
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
package org.veo.core.usecase.scope;

import javax.validation.Valid;

import org.veo.core.entity.Scope;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.repository.ScopeRepository;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.UseCase.IdAndClient;

import lombok.Value;

public class GetScopeUseCase
    implements TransactionalUseCase<IdAndClient, GetScopeUseCase.OutputData> {

  private final ScopeRepository scopeRepository;

  public GetScopeUseCase(ScopeRepository scopeRepository) {
    this.scopeRepository = scopeRepository;
  }

  @Override
  public OutputData execute(IdAndClient input) {
    Scope scope =
        scopeRepository
            .findById(input.getId())
            .orElseThrow(() -> new NotFoundException(input.getId(), Scope.class));
    scope.checkSameClient(input.getAuthenticatedClient());
    return new OutputData(scope);
  }

  @Valid
  @Value
  public static class OutputData implements UseCase.OutputData {
    @Valid Scope scope;
  }
}
