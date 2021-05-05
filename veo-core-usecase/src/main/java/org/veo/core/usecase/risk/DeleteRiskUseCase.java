/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Alexander Koderman.
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
package org.veo.core.usecase.risk;

import java.util.UUID;

import javax.transaction.Transactional;
import javax.validation.Valid;

import org.veo.core.entity.Client;
import org.veo.core.entity.Key;
import org.veo.core.entity.RiskAffected;
import org.veo.core.repository.RepositoryProvider;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;

import lombok.Value;

public class DeleteRiskUseCase
        implements TransactionalUseCase<DeleteRiskUseCase.InputData, UseCase.EmptyOutput> {

    private final RepositoryProvider repositoryProvider;

    public DeleteRiskUseCase(RepositoryProvider repositoryProvider) {
        this.repositoryProvider = repositoryProvider;
    }

    @Transactional
    @Override
    public EmptyOutput execute(InputData input) {
        var riskAffected = repositoryProvider.getRepositoryFor(input.entityClass)
                                             .findById(input.getRiskAffectedRef())
                                             .orElseThrow();

        riskAffected.checkSameClient(input.authenticatedClient);
        riskAffected.getRisk(input.scenarioRef)
                    .orElseThrow()
                    .remove();

        return EmptyOutput.INSTANCE;
    }

    @Valid
    @Value
    public static class InputData implements UseCase.InputData {
        Class<? extends RiskAffected<?, ?>> entityClass;
        Client authenticatedClient;
        Key<UUID> riskAffectedRef;
        Key<UUID> scenarioRef;
    }
}
