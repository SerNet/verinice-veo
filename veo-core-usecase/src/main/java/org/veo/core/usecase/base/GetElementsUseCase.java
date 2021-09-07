/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Jochen Kemnade.
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
package org.veo.core.usecase.base;

import java.util.UUID;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.veo.core.entity.Client;
import org.veo.core.entity.Element;
import org.veo.core.entity.Key;
import org.veo.core.repository.ClientRepository;
import org.veo.core.repository.ElementQuery;
import org.veo.core.repository.ElementRepository;
import org.veo.core.repository.PagedResult;
import org.veo.core.repository.PagingConfiguration;
import org.veo.core.repository.QueryCondition;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.UseCaseTools;

import lombok.Value;
import lombok.experimental.NonFinal;

/**
 * Reinstantiate persisted entity objects.
 */
public abstract class GetElementsUseCase<T extends Element, I extends GetElementsUseCase.InputData>
        implements TransactionalUseCase<I, GetElementsUseCase.OutputData<T>> {

    private final ElementRepository<T> repository;
    private final ClientRepository clientRepository;
    private final UnitHierarchyProvider unitHierarchyProvider;

    public GetElementsUseCase(ClientRepository clientRepository, ElementRepository<T> repository,
            UnitHierarchyProvider unitHierarchyProvider) {
        this.clientRepository = clientRepository;
        this.repository = repository;
        this.unitHierarchyProvider = unitHierarchyProvider;
    }

    /**
     * Find persisted control objects and reinstantiate them. Throws a domain
     * exception if the (optional) requested parent unit was not found in the
     * repository.
     */
    @Override
    public OutputData<T> execute(I input) {
        Client client = UseCaseTools.checkClientExists(input.getAuthenticatedClient()
                                                            .getId(),
                                                       clientRepository);

        var query = createQuery(client, input);
        applyDefaultQueryParameters(input, query);
        return new OutputData<>(query.execute(input.getPagingConfiguration()));
    }

    protected ElementQuery<T> createQuery(Client client, I input) {
        return repository.query(client);
    }

    private void applyDefaultQueryParameters(I input, ElementQuery<T> query) {
        if (input.getUnitUuid() != null) {
            query.whereUnitIn(input.getUnitUuid()
                                   .getValues()
                                   .stream()
                                   .flatMap((
                                           Key<UUID> rootUnitId) -> unitHierarchyProvider.findAllInRoot(rootUnitId)
                                                                                         .stream())
                                   .collect(Collectors.toSet()));
        }
        if (input.getSubType() != null) {
            query.whereSubTypeMatches(input.getSubType());
        }

        if (input.getDisplayName() != null) {
            query.whereDisplayNameMatchesIgnoringCase(input.getDisplayName());
        }
        if (input.getDescription() != null) {
            query.whereDescriptionMatchesIgnoreCase(input.getDescription());
        }

        if (input.getDesignator() != null) {
            query.whereDesignatorMatchesIgnoreCase(input.getDesignator());
        }

        if (input.getName() != null) {
            query.whereNameMatchesIgnoreCase(input.getName());
        }

        if (input.getUpdatedBy() != null) {
            query.whereUpdatedByContainsIgnoreCase(input.getUpdatedBy());
        }
    }

    @Valid
    @Value
    @NonFinal
    public static class InputData implements UseCase.InputData {
        Client authenticatedClient;
        QueryCondition<Key<UUID>> unitUuid;
        QueryCondition<String> displayName;
        QueryCondition<String> subType;
        QueryCondition<String> description;
        QueryCondition<String> designator;
        QueryCondition<String> name;
        QueryCondition<String> updatedBy;
        PagingConfiguration pagingConfiguration;
    }

    @Valid
    @Value
    public static class OutputData<T> implements UseCase.OutputData {
        @Valid
        PagedResult<T> elements;
    }
}