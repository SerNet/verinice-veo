/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Jonas Jordan
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
package org.veo.core.entity.decision;

import java.math.BigDecimal;

import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;
import org.veo.core.entity.ProcessRisk;
import org.veo.core.entity.RiskAffected;
import org.veo.core.entity.risk.RiskRef;

import lombok.Data;

/**
 * Provides the highest effective risk value affecting given element. Returns
 * null if the element is not affected by any risks or if none of the risks have
 * an effective risk value.
 */
@Data
public class MaxRiskProvider implements InputProvider {
    @Override
    public Object getValue(Element element, Domain domain) {
        if (element instanceof RiskAffected) {
            return getMaxRisk((RiskAffected) element, domain);
        }
        return null;
    }

    private BigDecimal getMaxRisk(RiskAffected<?, ?> element, Domain domain) {
        // TODO VEO-1295 apply domain parameter
        return element.getRisks()
                      .stream()
                      // TODO VEO-209 remove filter and cast once all types of risk can provide risk
                      // values
                      .filter(ProcessRisk.class::isInstance)
                      .map(ProcessRisk.class::cast)
                      .flatMap(risk -> risk.getRiskDefinitions()
                                           .stream()
                                           .map(rd -> risk.getRiskProvider(rd)))
                      .flatMap(provider -> provider.getCategorizedRisks()
                                                   .stream())
                      .map(determinedRisk -> determinedRisk.getEffectiveRisk())
                      .filter(it -> it != null)
                      .map(RiskRef::getIdRef)
                      .max(BigDecimal::compareTo)
                      .orElse(null);
    }
}
