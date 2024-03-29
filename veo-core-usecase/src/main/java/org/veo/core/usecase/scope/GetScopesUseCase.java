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

import org.veo.core.entity.Client;
import org.veo.core.entity.Scope;
import org.veo.core.repository.ClientRepository;
import org.veo.core.repository.ScopeRepository;
import org.veo.core.usecase.UseCaseTools;
import org.veo.core.usecase.base.GetElementsUseCase;
import org.veo.core.usecase.base.UnitHierarchyProvider;

/** Reinstantiate persisted scope objects. */
public class GetScopesUseCase
    extends GetElementsUseCase<Scope, GetElementsUseCase.RiskAffectedInputData> {

  public GetScopesUseCase(
      ClientRepository clientRepository,
      ScopeRepository scopeRepository,
      UnitHierarchyProvider unitHierarchyProvider) {
    super(clientRepository, scopeRepository, unitHierarchyProvider);
  }

  @Override
  public OutputData<Scope> execute(GetElementsUseCase.RiskAffectedInputData input) {
    Client client =
        UseCaseTools.checkClientExists(input.getAuthenticatedClient().getId(), clientRepository);
    var query = createQuery(client);
    if (input.isEmbedRisks()) {
      query.fetchRisks();
    }
    applyDefaultQueryParameters(input, query);
    return new OutputData<>(query.execute(input.getPagingConfiguration()));
  }
}
