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

import java.util.List;

import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainBase;
import org.veo.core.entity.Element;
import org.veo.core.entity.ElementType;
import org.veo.core.entity.RiskAffected;
import org.veo.core.entity.type.VeoType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ControlImplementationsExpression implements VeoExpression {
  private boolean complianceOnly;

  @Override
  public Object getValue(Element element, Domain domain) {
    if (element instanceof RiskAffected<?, ?> ra) {
      var complianceSubTypes =
          domain.getControlImplementationConfiguration().complianceControlSubTypes();
      return ra.getControlImplementations().stream()
          .filter(
              ci ->
                  !complianceOnly
                      || ci.getControl()
                          .findSubType(domain)
                          .filter(complianceSubTypes::contains)
                          .isPresent())
          .toList();
    }
    return List.of();
  }

  @Override
  public void selfValidate(DomainBase domain, ElementType elementType) {
    VeoType.element(elementType)
        .mustBeIncludedIn(VeoType.riskAffected(), "cannot determine control implementations");
  }

  @Override
  public VeoType getValueType(DomainBase domain, ElementType elementType) {
    return VeoType.listOf(VeoType.controlImplementation());
  }
}
