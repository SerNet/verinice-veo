/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Alexander Koderman
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
package org.veo.adapter.presenter.api.io.mapper;

import java.util.Optional;

import org.veo.core.entity.DomainTemplate;
import org.veo.core.entity.risk.CategoryRef;
import org.veo.core.entity.risk.ImpactRef;
import org.veo.core.entity.risk.ImplementationStatusRef;
import org.veo.core.entity.risk.ProbabilityRef;
import org.veo.core.entity.risk.ReferenceProvider;
import org.veo.core.entity.risk.RiskDefinitionRef;
import org.veo.core.entity.risk.RiskRef;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DomainRiskReferenceProvider extends ReferenceProvider {

    @Getter
    private DomainTemplate domain;

    public static DomainRiskReferenceProvider referencesForDomain(DomainTemplate domain) {
        return new DomainRiskReferenceProvider(domain);
    }

    @Override
    public Optional<RiskRef> getRiskRef(String riskDefinitionId, String category, String riskId) {
        // TODO VEO-1104 ensure valid riskdefinition constraints
        return Optional.ofNullable(createRiskRef(riskId));
    }

    @Override
    public Optional<ProbabilityRef> getProbabilityRef(String riskDefinitionId,
            String probabilityId) {
        // TODO VEO-1104 ensure valid riskdefinition constraints
        return Optional.ofNullable(createProbabilityRef(probabilityId));
    }

    @Override
    public Optional<ImpactRef> getImpactRef(String riskDefinitionId, String category,
            String probabilityId) {
        // TODO VEO-1104 ensure valid riskdefinition constraints
        return Optional.ofNullable(createImpactRef(probabilityId));
    }

    @Override
    public Optional<CategoryRef> getCategoryRef(String riskDefinitionId, String categoryId) {
        // TODO VEO-1104 ensure valid riskdefinition constraints
        return Optional.ofNullable(createCategoryRef(categoryId));
    }

    @Override
    public Optional<ImplementationStatusRef> getImplementationStatus(String riskDefinitionId,
            int ordinalValue) {
        return domain.getRiskDefinition(riskDefinitionId)
                     .flatMap(rd -> rd.getImplementationStateDefinition()
                                      .getLevel(ordinalValue))
                     .map(level -> createImplementationStatusRef(level.getOrdinalValue()));
    }

    @Override
    public Optional<RiskDefinitionRef> getRiskDefinitionRef(String riskDefinitionId) {
        return domain.getRiskDefinition(riskDefinitionId)
                     .map(RiskDefinitionRef::from);
    }
}