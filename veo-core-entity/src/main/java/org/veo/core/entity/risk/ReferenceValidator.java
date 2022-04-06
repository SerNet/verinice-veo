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
 * Offers functionality to validate provided reference objects against a
 * risk-definition.
 * <p>
 * The implementing provider must make sure that the requested reference object
 * is valid in each context.
 */
public abstract class ReferenceValidator extends RiskReferenceFactory {
    /**
     * Validates the given reference against the risk-definition.
     *
     * @param probabilityRef
     *            a probability reference to validate
     * @return the same probability-reference if it is valid. Returns {@code null}
     *         if the input is {@code null}.
     * @throws org.veo.core.entity.code.EntityValidationException
     *             if the reference is not valid for the context
     */
    public abstract ProbabilityRef validate(ProbabilityRef probabilityRef);

    /**
     * Validates the given reference against the risk-definition.
     *
     * @param category
     *            the impact category in which the level is defined
     * @param impactRef
     *            an impact reference to validate
     * @return the same impact-reference if it is valid. Returns {@code null} if the
     *         input is {@code null}.
     * @throws org.veo.core.entity.code.EntityValidationException
     *             if the reference is not valid for the context
     */
    public abstract ImpactRef validate(CategoryRef category, ImpactRef impactRef);

    /**
     * Validates the given reference against the risk-definition.
     *
     * @param category
     *            the impact category in which the level is defined
     * @param riskRef
     *            a risk-reference to validate
     * @return the same risk-reference if it is valid. Returns {@code null} if the
     *         input is {@code null}.
     * @throws org.veo.core.entity.code.EntityValidationException
     *             if the reference is not valid for the context
     */
    public abstract RiskRef validate(CategoryRef category, RiskRef riskRef);
}
