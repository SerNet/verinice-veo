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
package org.veo.core.usecase.process;

import org.veo.core.entity.Client;
import org.veo.core.entity.Process;
import org.veo.core.repository.ClientRepository;
import org.veo.core.repository.ProcessRepository;
import org.veo.core.usecase.UseCaseTools;
import org.veo.core.usecase.base.GetElementsUseCase;
import org.veo.core.usecase.base.UnitHierarchyProvider;

/** Reinstantiate persisted process objects. */
public class GetProcessesUseCase
    extends GetElementsUseCase<Process, GetElementsUseCase.RiskAffectedInputData> {

  public GetProcessesUseCase(
      ClientRepository clientRepository,
      ProcessRepository repository,
      UnitHierarchyProvider unitHierarchyProvider) {
    super(clientRepository, repository, unitHierarchyProvider);
  }

  @Override
  public OutputData<Process> execute(GetElementsUseCase.RiskAffectedInputData input) {
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
