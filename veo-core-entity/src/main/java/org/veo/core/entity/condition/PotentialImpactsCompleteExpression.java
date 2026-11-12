/*
 * verinice.veo
 * Copyright (C) 2026  Aziz Khalledi
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
 */
package org.veo.core.entity.condition;

import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainBase;
import org.veo.core.entity.Element;
import org.veo.core.entity.ElementType;
import org.veo.core.entity.RiskAffected;
import org.veo.core.entity.event.ElementEvent;
import org.veo.core.entity.event.RiskAffectingElementChangeEvent;
import org.veo.core.entity.event.RiskEvent;
import org.veo.core.entity.risk.CategoryRef;
import org.veo.core.entity.risk.ImpactValues;
import org.veo.core.entity.risk.RiskDefinitionRef;
import org.veo.core.entity.riskdefinition.RiskDefinition;
import org.veo.core.entity.type.VeoType;

import lombok.Data;

@Data
public class PotentialImpactsCompleteExpression implements VeoExpression {
  @Override
  public Object getValue(Element element, Domain domain) {
    if (element instanceof RiskAffected<?, ?> ra) {
      return domain.getRiskDefinitions().values().stream()
          .allMatch(rd -> isComplete(ra, domain, rd));
    }
    return false;
  }

  @Override
  public boolean isAffectedByEvent(ElementEvent event, Domain domain) {
    return event instanceof RiskAffectingElementChangeEvent re
        && re.getChanges().contains(RiskEvent.ChangedValues.IMPACT_VALUES_CHANGED);
  }

  private static boolean isComplete(RiskAffected<?, ?> element, Domain domain, RiskDefinition rd) {
    var effectiveImpacts =
        element
            .getImpactValues(domain, RiskDefinitionRef.from(rd))
            .map(ImpactValues::getPotentialImpactsEffective);
    return rd.getCategories().stream()
        .filter(cat -> !cat.getPotentialImpacts().isEmpty())
        .map(CategoryRef::from)
        .allMatch(cat -> effectiveImpacts.map(i -> i.containsKey(cat)).orElse(false));
  }

  @Override
  public void selfValidate(DomainBase domain, ElementType elementType) {
    VeoType.element(elementType)
        .mustBeIncludedIn(VeoType.riskAffected(), "cannot determine potential impacts");
  }

  @Override
  public VeoType getValueType(DomainBase domain, ElementType elementType) {
    return VeoType.bool();
  }
}
