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
package org.veo.core.entity.risk;

import org.veo.core.entity.exception.RiskConsistencyException;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class DomainRiskReferenceValidator extends ReferenceValidator {

    private final DomainRiskReferenceProvider refProvider;

    private final RiskDefinitionRef riskDefinition;

    @Override
    public ProbabilityRef validate(ProbabilityRef probabilityRef) {
        return probabilityRef == null ? null
                : refProvider.getProbabilityRef(riskDefinition.getIdRef(),
                                                probabilityRef.getIdRef())
                             .orElseThrow(() -> new RiskConsistencyException(
                                     String.format("Probability reference %s is not valid in the present context.",
                                                   probabilityRef.getIdRef())));
    }

    @Override
    public ImpactRef validate(CategoryRef category, ImpactRef impactRef) {
        return impactRef == null ? null
                : refProvider.getImpactRef(riskDefinition.getIdRef(), category.getIdRef(),
                                           impactRef.getIdRef())
                             .orElseThrow(() -> new RiskConsistencyException(
                                     String.format("Impact reference %s for category %s is not valid in the present context.",
                                                   impactRef.getIdRef(), category.getIdRef())));
    }

    @Override
    public RiskRef validate(CategoryRef category, RiskRef riskRef) {
        return riskRef == null ? null
                : refProvider.getRiskRef(riskDefinition.getIdRef(), riskRef.getIdRef())
                             .orElseThrow(() -> new RiskConsistencyException(
                                     String.format("Risk reference %s for category %s is not valid in the present context.",
                                                   riskRef.getIdRef(), category.getIdRef())));
    }
}