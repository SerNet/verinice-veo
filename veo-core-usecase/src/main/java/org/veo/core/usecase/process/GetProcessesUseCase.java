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

import java.util.UUID;

import javax.validation.Valid;

import org.veo.core.entity.Client;
import org.veo.core.entity.Key;
import org.veo.core.entity.Process;
import org.veo.core.repository.ClientRepository;
import org.veo.core.repository.ElementQuery;
import org.veo.core.repository.PagingConfiguration;
import org.veo.core.repository.ProcessRepository;
import org.veo.core.repository.QueryCondition;
import org.veo.core.repository.SingleValueQueryCondition;
import org.veo.core.usecase.UseCaseTools;
import org.veo.core.usecase.base.GetElementsUseCase;
import org.veo.core.usecase.base.UnitHierarchyProvider;

import lombok.EqualsAndHashCode;
import lombok.Value;

/** Reinstantiate persisted process objects. */
public class GetProcessesUseCase
    extends GetElementsUseCase<Process, GetProcessesUseCase.InputData> {

  private final ProcessRepository processRepository;

  public GetProcessesUseCase(
      ClientRepository clientRepository,
      ProcessRepository repository,
      UnitHierarchyProvider unitHierarchyProvider) {
    super(clientRepository, repository, unitHierarchyProvider);
    this.processRepository = repository;
  }

  @Override
  public OutputData<Process> execute(GetProcessesUseCase.InputData input) {
    Client client =
        UseCaseTools.checkClientExists(input.getAuthenticatedClient().getId(), clientRepository);
    var query = createQuery(client, input.isEmbedRisks());
    applyDefaultQueryParameters(input, query);
    return new OutputData<>(query.execute(input.getPagingConfiguration()));
  }

  private ElementQuery<Process> createQuery(Client client, boolean withRisks) {
    return processRepository.query(client, withRisks);
  }

  @Valid
  @Value
  @EqualsAndHashCode(callSuper = true)
  public static class InputData extends GetElementsUseCase.InputData {
    boolean embedRisks;

    public InputData(
        Client authenticatedClient,
        QueryCondition<Key<UUID>> unitUuid,
        QueryCondition<String> displayName,
        QueryCondition<String> subType,
        QueryCondition<String> status,
        QueryCondition<Key<UUID>> childElementIds,
        SingleValueQueryCondition<Boolean> hasChildElements,
        SingleValueQueryCondition<Boolean> hasParentElements,
        QueryCondition<String> description,
        QueryCondition<String> designator,
        QueryCondition<String> name,
        QueryCondition<String> updatedBy,
        PagingConfiguration pagingConfiguration,
        boolean embedRisks) {
      super(
          authenticatedClient,
          unitUuid,
          displayName,
          subType,
          status,
          childElementIds,
          hasChildElements,
          hasParentElements,
          description,
          designator,
          name,
          updatedBy,
          pagingConfiguration);
      this.embedRisks = embedRisks;
    }
  }
}
