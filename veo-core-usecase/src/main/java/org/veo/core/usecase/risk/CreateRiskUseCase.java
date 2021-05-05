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

import javax.transaction.Transactional;

import org.veo.core.entity.AbstractRisk;
import org.veo.core.entity.Domain;
import org.veo.core.entity.RiskAffected;
import org.veo.core.entity.Scenario;
import org.veo.core.repository.RepositoryProvider;

public class CreateRiskUseCase<T extends RiskAffected<T, R>, R extends AbstractRisk<T, R>>
        extends AbstractRiskUseCase<T, R> {

    private final Class<T> entityClass;

    public CreateRiskUseCase(Class<T> entityClass, RepositoryProvider repositoryProvider) {
        super(repositoryProvider);
        this.entityClass = entityClass;
    }

    @Transactional
    @Override
    public OutputData<R> execute(InputData input) {
        // Retrieve the necessary entities for the requested operation:
        var riskAffected = findEntity(entityClass, input.getRiskAffectedRef()).orElseThrow();

        var scenario = findEntity(Scenario.class, input.getScenarioRef()).orElseThrow();

        var domains = findEntities(Domain.class, input.getDomainRefs());

        // Validate security constraints:
        riskAffected.checkSameClient(input.getAuthenticatedClient());
        scenario.checkSameClient(input.getAuthenticatedClient());
        checkDomainOwnership(input.getAuthenticatedClient(), domains);

        // Apply requested operation:
        var risk = riskAffected.newRisk(scenario, domains);

        risk = applyOptionalInput(input, risk);

        return new OutputData<>(risk);
    }
}
