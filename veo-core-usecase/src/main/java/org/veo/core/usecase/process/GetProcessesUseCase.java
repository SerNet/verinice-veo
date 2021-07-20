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
import org.veo.core.entity.Process.Status;
import org.veo.core.repository.ClientRepository;
import org.veo.core.repository.PagingConfiguration;
import org.veo.core.repository.ProcessQuery;
import org.veo.core.repository.ProcessRepository;
import org.veo.core.usecase.base.GetEntitiesUseCase;
import org.veo.core.usecase.base.QueryCondition;
import org.veo.core.usecase.base.UnitHierarchyProvider;

import lombok.EqualsAndHashCode;
import lombok.Value;

/**
 * Reinstantiate persisted process objects.
 */
public class GetProcessesUseCase
        extends GetEntitiesUseCase<Process, GetProcessesUseCase.InputData> {

    private final ProcessRepository repository;

    public GetProcessesUseCase(ClientRepository clientRepository, ProcessRepository repository,
            UnitHierarchyProvider unitHierarchyProvider) {
        super(clientRepository, repository, unitHierarchyProvider);
        this.repository = repository;
    }

    @Override
    protected ProcessQuery createQuery(Client client, InputData input) {
        var query = repository.query(client);
        if (input.getStatus() != null) {
            query.whereStatusIn(input.getStatus()
                                     .getValues());
        }
        return query;
    }

    @Valid
    @Value
    @EqualsAndHashCode(callSuper = true)
    public static class InputData extends GetEntitiesUseCase.InputData {

        public InputData(Client authenticatedClient, QueryCondition<Key<UUID>> unitUuid,
                QueryCondition<String> displayName, QueryCondition<String> subType,
                QueryCondition<Status> status, PagingConfiguration pagingConfiguration) {
            super(authenticatedClient, unitUuid, displayName, subType, pagingConfiguration);
            this.status = status;
        }

        QueryCondition<Status> status;
    }

}