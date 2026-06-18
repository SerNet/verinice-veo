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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.constraints.NotNull;

import org.veo.core.entity.CustomLink;
import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainBase;
import org.veo.core.entity.Element;
import org.veo.core.entity.ElementType;
import org.veo.core.entity.definitions.LinkDefinition;
import org.veo.core.entity.state.ElementTypeDefinitionState;
import org.veo.core.entity.type.VeoType;

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
  public void selfValidate(DomainBase domain, ElementType elementType) {
    var sourcesType = sources.getValueType(domain, elementType);
    sourcesType.mustBeListOrNothing("invalid link sources");
    getValueType(domain, elementType);
  }

  @Override
  public VeoType getValueType(DomainBase domain, ElementType elementType) {
    var sourcesType = sources.getValueType(domain, elementType);
    return sourcesType
        .findListItemType()
        .map(
            linkSourceType -> {
              var possibleLinkTypes =
                  Arrays.stream(ElementType.values())
                      .filter(t -> linkSourceType.includes(VeoType.element(t)))
                      .map(domain::getElementTypeDefinition)
                      .map(ElementTypeDefinitionState::getLinks)
                      .map(Map::entrySet)
                      .flatMap(Collection::stream)
                      .filter(linkEntry -> linkType == null || linkEntry.getKey().equals(linkType))
                      .map(Map.Entry::getValue);
              var possibleLinkTargetTypes =
                  possibleLinkTypes
                      .map(LinkDefinition::getTargetType)
                      .map(VeoType::element)
                      .toList();
              if (possibleLinkTargetTypes.isEmpty()) {
                throw new IllegalArgumentException(
                    "No links defined for element type %s and %s"
                        .formatted(
                            linkSourceType,
                            Optional.ofNullable(linkType)
                                .map("link type '%s'"::formatted)
                                .orElse("any link type")));
              }
              return VeoType.listOf(VeoType.sumOf(possibleLinkTargetTypes));
            })
        .orElse(VeoType.listOf(VeoType.nothing()));
  }
}
