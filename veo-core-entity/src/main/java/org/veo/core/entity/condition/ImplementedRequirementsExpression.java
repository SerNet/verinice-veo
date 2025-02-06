/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2024  Jonas Jordan
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

import java.util.Collection;
import java.util.HashSet;
import java.util.stream.Collectors;

import jakarta.validation.constraints.NotNull;

import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainBase;
import org.veo.core.entity.Element;
import org.veo.core.entity.ElementType;
import org.veo.core.entity.RiskAffected;
import org.veo.core.entity.compliance.RequirementImplementation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class ImplementedRequirementsExpression implements VeoExpression {
  @NotNull VeoExpression riskAffected;

  @Override
  public Object getValue(Element element, Domain domain) {
    if (riskAffected.getValue(element, domain) instanceof RiskAffected<?, ?> ra) {
      return ra.getRequirementImplementations().stream()
          .map(RequirementImplementation::getControl)
          .collect(Collectors.toSet());
    }
    return new HashSet<>();
  }

  @Override
  public void selfValidate(DomainBase domain, ElementType elementType) {
    riskAffected.selfValidate(domain, elementType);
    var valueType = riskAffected.getValueType(domain, elementType);
    if (!RiskAffected.class.isAssignableFrom(valueType)) {
      throw new IllegalArgumentException(
          "Cannot get implemented requirements of %s".formatted(valueType));
    }
  }

  @Override
  public Class<?> getValueType(DomainBase domain, ElementType elementType) {
    return Collection.class;
  }
}
