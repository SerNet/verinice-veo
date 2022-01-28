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

/**
 * The effects of a threat event interfering with an asset / a process / a
 * scope.
 *
 * @see org.veo.core.entity.riskdefinition.RiskDefinition
 */
public interface Impact {

    /**
     * The impact for a specific combination of scenario and process/asset/scope. It
     * may be higher or lower (but usually should not be higher) than the potential
     * impact that was initially estimated.
     */
    void setSpecificImpact(ImpactRef specific);

    /**
     * The potential impact on one of the information security properties of an
     * asset (or a process/scope).
     * <p>
     * This is taken from the risk-affected entity (i.e. process) and does not yet
     * take individual scenarios into account. It is solely based on evaluation the
     * data processed by/on the process/the asset.
     */
    ImpactRef getPotentialImpact();

    /**
     * The impact for a specific combination of scenario and process/asset/scope. It
     * may be higher or lower (but usually should not be higher) than the potential
     * impact that was initially estimated.
     */
    ImpactRef getSpecificImpact();

    /**
     * The result of taking both the potential and the specific impact into account.
     */
    ImpactRef getEffectiveImpact();

    String getSpecificImpactExplanation();

    /**
     * The potential impact on one of the information security properties of an
     * asset (or a process/scope).
     * <p>
     * This is taken from the risk-affected entity (i.e. process) and does not yet
     * take individual scenarios into account. It is solely based on evaluation the
     * data processed by/on the process/the asset.
     */
    void setPotentialImpact(ImpactRef potential);

    void setSpecificImpactExplanation(String explanation);
}
