/*******************************************************************************
 * Copyright (c) 2019 Alexander Koderman.
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.veo.core.usecase.risk;

import java.util.Collection;
import java.util.UUID;

import javax.transaction.Transactional;
import javax.validation.Valid;

import org.veo.core.entity.AbstractRisk;
import org.veo.core.entity.Client;
import org.veo.core.entity.Key;
import org.veo.core.entity.RiskAffected;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.repository.Repository;
import org.veo.core.usecase.repository.RepositoryProvider;

import lombok.Value;

public class GetRisksUseCase<T extends RiskAffected<T, R>, R extends AbstractRisk<T, R>>
        implements TransactionalUseCase<GetRisksUseCase.InputData, GetRisksUseCase.OutputData<R>> {

    private final RepositoryProvider repositoryProvider;
    private final Class<T> entityClass;

    public GetRisksUseCase(RepositoryProvider repositoryProvider, Class<T> entityClass) {
        this.repositoryProvider = repositoryProvider;
        this.entityClass = entityClass;
    }

    @Transactional
    public OutputData<R> execute(InputData input) {
        Repository<T, Key<UUID>> repositoryFor = repositoryProvider.getRepositoryFor(entityClass);
        var riskAffected = repositoryFor.findById(input.riskAffectedRef)

                                        .orElseThrow();

        riskAffected.checkSameClient(input.authenticatedClient);

        return new OutputData<>(riskAffected.getRisks());
    }

    @Valid
    @Value
    public static class InputData implements UseCase.InputData {
        Client authenticatedClient;
        Key<UUID> riskAffectedRef;
    }

    @Valid
    @Value
    public static class OutputData<R> implements UseCase.OutputData {
        @Valid
        Collection<R> risks;
    }
}
