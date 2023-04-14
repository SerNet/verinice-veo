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
package org.veo.core.entity.condition;

import static org.veo.core.entity.event.RiskEvent.ChangedValues.RISK_CREATED;
import static org.veo.core.entity.event.RiskEvent.ChangedValues.RISK_DELETED;
import static org.veo.core.entity.event.RiskEvent.ChangedValues.RISK_VALUES_CHANGED;

import java.math.BigDecimal;
import java.util.Objects;

import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainBase;
import org.veo.core.entity.Element;
import org.veo.core.entity.ProcessRisk;
import org.veo.core.entity.RiskAffected;
import org.veo.core.entity.event.ElementEvent;
import org.veo.core.entity.event.RiskAffectingElementChangeEvent;
import org.veo.core.entity.risk.DeterminedRisk;
import org.veo.core.entity.risk.RiskRef;

import lombok.Data;

/**
 * Provides the highest residual risk value affecting given element. Returns null if the element is
 * not affected by any risks or if none of the risks have a residual risk value.
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

  @Override
  public boolean isAffectedByEvent(ElementEvent event, Domain domain) {
    if (event instanceof RiskAffectingElementChangeEvent) {
      var elementRiskEvent = (RiskAffectingElementChangeEvent) event;
      return elementRiskEvent.getChangedRisks().stream()
          .anyMatch(
              riskChangedEvent -> {
                var changes = riskChangedEvent.getChanges();
                // Reevaluate max risk if a risk has been created or removed.
                if (changes.contains(RISK_CREATED) || changes.contains(RISK_DELETED)) {
                  return true;
                }
                // Reevaluate max risk if a risk value has been changed in the domain.
                if (changes.contains(RISK_VALUES_CHANGED)
                    && riskChangedEvent.getDomainId().equals(domain.getId())) {
                  return true;
                }
                return false;
              });
    }
    return false;
  }

  @Override
  public Class<?> getValueType(DomainBase domain, String elementType) {
    return BigDecimal.class;
  }

  private BigDecimal getMaxRisk(RiskAffected<?, ?> element, Domain domain) {
    return element.getRisks().stream()
        // TODO VEO-209 remove filter and cast once all types of risk can provide risk
        // values
        .filter(ProcessRisk.class::isInstance)
        .map(ProcessRisk.class::cast)
        .flatMap(
            risk ->
                risk.getRiskDefinitions(domain).stream()
                    .map(rd -> risk.getRiskProvider(rd, domain)))
        .flatMap(provider -> provider.getCategorizedRisks().stream())
        .map(DeterminedRisk::getResidualRisk)
        .filter(Objects::nonNull)
        .map(RiskRef::getIdRef)
        .max(BigDecimal::compareTo)
        .orElse(null);
  }
}
