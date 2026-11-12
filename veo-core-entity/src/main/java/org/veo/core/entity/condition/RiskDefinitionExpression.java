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
import org.veo.core.entity.Scope;
import org.veo.core.entity.event.ElementEvent;
import org.veo.core.entity.risk.RiskDefinitionRef;
import org.veo.core.entity.type.VeoType;

import lombok.Data;

@Data
public class RiskDefinitionExpression implements VeoExpression {
  @Override
  public Object getValue(Element element, Domain domain) {
    if (element instanceof Scope scope) {
      return scope.getRiskDefinition(domain).map(RiskDefinitionRef::getIdRef).orElse(null);
    }
    return null;
  }

  @Override
  public boolean isAffectedByEvent(ElementEvent event, Domain domain) {
    return false;
  }

  @Override
  public void selfValidate(DomainBase domain, ElementType elementType) {
    VeoType.element(elementType)
        .mustBeIncludedIn(VeoType.element(ElementType.SCOPE), "cannot determine risk definition");
  }

  @Override
  public VeoType getValueType(DomainBase domain, ElementType elementType) {
    return VeoType.string().orNothing();
  }
}
