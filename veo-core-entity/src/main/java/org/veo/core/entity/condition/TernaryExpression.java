/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2026  Jochen Kemnade
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

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainBase;
import org.veo.core.entity.Element;
import org.veo.core.entity.ElementType;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Evaluates a condition and returns either value depending on the result. */
@Data
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TernaryExpression implements VeoExpression {
  @JsonProperty("if")
  private VeoExpression condition;

  @JsonProperty("then")
  private VeoExpression thenValue;

  @JsonProperty("else")
  private VeoExpression elseValue;

  @Override
  public Object getValue(Element element, Domain domain) {
    Object conditionValue = condition.getValue(element, domain);
    if (Boolean.TRUE.equals(conditionValue)) {
      return thenValue.getValue(element, domain);
    } else {
      return elseValue.getValue(element, domain);
    }
  }

  @Override
  public void selfValidate(DomainBase domain, ElementType elementType) {
    condition.selfValidate(domain, elementType);
    thenValue.selfValidate(domain, elementType);
    elseValue.selfValidate(domain, elementType);
    var conditionType = condition.getValueType(domain, elementType);
    if (!Boolean.class.isAssignableFrom(conditionType)) {
      throw new IllegalArgumentException(
          "Cannot use %s as 'condition' type.".formatted(conditionType.getSimpleName()));
    }
    var thenValueType = thenValue.getValueType(domain, elementType);
    var elseValueType = elseValue.getValueType(domain, elementType);
    if (thenValueType != null
        && elseValueType != null
        && !Objects.equals(thenValueType, elseValueType)) {
      throw new IllegalArgumentException(
          "Cannot use differently typed values for 'then' (%s) and 'else' (%s)"
              .formatted(thenValueType.getSimpleName(), elseValueType.getSimpleName()));
    }
  }

  @Override
  public Class<?> getValueType(DomainBase domain, ElementType elementType) {
    return thenValue.getValueType(domain, elementType);
  }
}
