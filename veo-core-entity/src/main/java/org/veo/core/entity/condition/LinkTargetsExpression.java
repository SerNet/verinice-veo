/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Jonas Jordan
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
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.constraints.NotNull;

import org.veo.core.entity.CustomLink;
import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainBase;
import org.veo.core.entity.Element;
import org.veo.core.entity.exception.UnprocessableDataException;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Gathers a flat list of link targets from a collection of source elements. Can filter by link
 * type.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LinkTargetsExpression implements VeoExpression {
  @NotNull private VeoExpression sources;
  private String linkType;

  @Override
  public Set<Element> getValue(Element element, Domain domain) {
    var result = sources.getValue(element, domain);
    if (result instanceof Collection<?> c) {
      return c.stream()
          .filter(i -> i instanceof Element)
          .map(i -> (Element) i)
          .flatMap(e -> e.getLinks(domain).stream())
          .filter(l -> linkType == null || l.getType().equals(linkType))
          .map(CustomLink::loadTarget) // TODO #2863 use repository to fetch targets efficiently
          .collect(Collectors.toSet());
    }
    return new HashSet<>();
  }

  @Override
  public void selfValidate(DomainBase domain, String elementType) {
    var sourcesType = sources.getValueType(domain, elementType);
    if (Collection.class.isAssignableFrom(sourcesType)) {
      throw new UnprocessableDataException(
          "Cannot get link targets from %s, expected a collection of elements"
              .formatted(sourcesType));
    }
    if (linkType != null
        && domain.getElementTypeDefinition(elementType).findLink(linkType).isEmpty()) {
      throw new UnprocessableDataException(
          "Link type %s not defined for %s in domain %s".formatted(linkType, elementType, domain));
    }
  }

  @Override
  public Class<?> getValueType(DomainBase domain, String elementType) {
    return Boolean.class;
  }
}
