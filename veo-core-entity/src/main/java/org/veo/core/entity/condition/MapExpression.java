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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainBase;
import org.veo.core.entity.Element;
import org.veo.core.entity.ElementType;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Transform an input collection ("source") using a mapping. */
@Data
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MapExpression implements VeoExpression {
  private VeoExpression source;
  private ConstantExpression mapping;

  @Override
  public Object getValue(Element element, Domain domain) {
    Map m = (Map) mapping.getValue(element, domain);
    return Optional.ofNullable(source.getValue(element, domain))
        .map(Collection.class::cast)
        .map(coll -> coll.stream().map(m::get).collect(Collectors.toList()))
        .orElse(null);
  }

  @Override
  public void selfValidate(DomainBase domain, ElementType elementType) {
    source.selfValidate(domain, elementType);
    mapping.selfValidate(domain, elementType);
    var sourceType = source.getValueType(domain, elementType);
    if (!Collection.class.isAssignableFrom(sourceType)) {
      throw new IllegalArgumentException(
          "Cannot use %s as source type.".formatted(sourceType.getSimpleName()));
    }
    var mappingType = mapping.getValueType(domain, elementType);
    if (!Map.class.isAssignableFrom(mappingType)) {
      throw new IllegalArgumentException(
          "Cannot use %s as source type.".formatted(sourceType.getSimpleName()));
    }
  }

  @Override
  public Class<?> getValueType(DomainBase domain, ElementType elementType) {
    return List.class;
  }
}
