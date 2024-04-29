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

import org.veo.core.entity.Scope;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.repository.DomainRepository;
import org.veo.core.repository.ScopeRepository;
import org.veo.core.usecase.base.GetElementUseCase;

public class GetScopeUseCase extends GetElementUseCase<Scope> {
  private final ScopeRepository scopeRepository;

  public GetScopeUseCase(DomainRepository domainRepository, ScopeRepository repository) {
    super(domainRepository, repository, Scope.class);
    this.scopeRepository = repository;
  }

  @Override
  public GetElementUseCase.OutputData<Scope> execute(InputData input) {
    var scope =
        scopeRepository
            .findById(input.elementId(), input.embedRisks())
            .orElseThrow(() -> new NotFoundException(input.elementId(), Scope.class));
    scope.checkSameClient(input.authenticatedClient());
    return new GetElementUseCase.OutputData<>(scope, getDomain(scope, input).orElse(null));
  }
}
