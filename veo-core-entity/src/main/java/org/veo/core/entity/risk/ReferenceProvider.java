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

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Provides value objects with references to risk definition items. This should
 * remain the only way to create these references.
 * <p>
 * The implementing provider must make sure that the requested reference object
 * is valid in each context.
 */
public abstract class ReferenceProvider extends ReferenceFactory {
    /**
     * Returns a valid risk reference. Implementations must respect a valid risk
     * definition for the context.
     */
    public abstract Optional<RiskRef> getRiskRef(String riskDefinitionId, BigDecimal riskId);

    public abstract Optional<ProbabilityRef> getProbabilityRef(String riskDefinitionId,
            BigDecimal probabilityId);

    public abstract Optional<ImpactRef> getImpactRef(String riskDefinitionId, String category,
            BigDecimal probabilityId);

    public abstract Optional<CategoryRef> getCategoryRef(String riskDefinitionId,
            String categoryId);

    public abstract Optional<ImplementationStatusRef> getImplementationStatus(
            String riskDefinitionId, int ordinalValue);

    public abstract Optional<RiskDefinitionRef> getRiskDefinitionRef(String riskDefinitionId);
}
