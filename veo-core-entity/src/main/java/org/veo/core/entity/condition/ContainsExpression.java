/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2024  Jochen Kemnade
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
import java.util.Optional;

import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainBase;
import org.veo.core.entity.Element;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Returns whether a custom attribute ("haystack") contains a specific value ("needle"). */
@Data
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ContainsExpression implements VeoExpression {
  private CustomAspectAttributeValueExpression haystack;
  private ConstantExpression needle;

  @Override
  public Object getValue(Element element, Domain domain) {
    return Optional.ofNullable(haystack.getValue(element, domain))
        .map(Collection.class::cast)
        .map(it -> it.contains(needle.getValue(element, domain)))
        .orElse(false);
  }

  @Override
  public void selfValidate(DomainBase domain, String elementType) {
    needle.selfValidate(domain, elementType);
    haystack.selfValidate(domain, elementType);
    var hayStackType = haystack.getValueType(domain, elementType);
    if (!Collection.class.isAssignableFrom(hayStackType)) {
      throw new IllegalArgumentException(
          "Cannot use %s as haystack type.".formatted(hayStackType.getSimpleName()));
    }
  }

  @Override
  public Class<?> getValueType(DomainBase domain, String elementType) {
    return Boolean.class;
  }
}
