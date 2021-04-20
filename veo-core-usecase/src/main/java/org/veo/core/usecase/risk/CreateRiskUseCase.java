/*******************************************************************************
 * Copyright (c) 2020 Alexander Koderman.
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

import javax.transaction.Transactional;

import org.veo.core.entity.AbstractRisk;
import org.veo.core.entity.Domain;
import org.veo.core.entity.RiskAffected;
import org.veo.core.entity.Scenario;
import org.veo.core.repository.RepositoryProvider;

public class CreateRiskUseCase<T extends RiskAffected<T, R>, R extends AbstractRisk<T, R>>
        extends AbstractRiskUseCase<T, R> {

    private final RepositoryProvider repositoryProvider;
    private final Class<T> entityClass;

    public CreateRiskUseCase(Class<T> entityClass, RepositoryProvider repositoryProvider) {
        super(repositoryProvider);
        this.entityClass = entityClass;
        this.repositoryProvider = repositoryProvider;
    }

    @Transactional
    @Override
    public OutputData<R> execute(InputData input) {
        // Retrieve the necessary entities for the requested operation:
        var riskAffected = repositoryProvider.getRepositoryFor(entityClass)
                                             .findById(input.getRiskAffectedRef())
                                             .orElseThrow();

        var scenario = repositoryProvider.getRepositoryFor(Scenario.class)
                                         .findById(input.getScenarioRef())
                                         .orElseThrow();

        var domains = repositoryProvider.getRepositoryFor(Domain.class)
                                        .getByIds(input.getDomainRefs());

        // Validate security constraints:
        riskAffected.checkSameClient(input.getAuthenticatedClient());
        scenario.checkSameClient(input.getAuthenticatedClient());
        checkClients(input.getAuthenticatedClient(), domains);

        // Apply requested operation:
        var risk = riskAffected.newRisk(scenario, domains);

        risk = applyOptionalInput(input, risk);

        return new OutputData<>(risk);
    }
}
