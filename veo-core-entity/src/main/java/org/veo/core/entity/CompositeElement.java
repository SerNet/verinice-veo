/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Jochen Kemnade.
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
package org.veo.core.entity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.veo.core.entity.specification.EntitySpecification;

/**
 * An element whose parts can optionally be modeled as well
 *
 * @param <T> the type of the entity and its parts
 */
public interface CompositeElement<T extends CompositeElement<T>> extends Element {

  Set<T> getParts();

  Set<T> getComposites();

  default Set<T> findPartsFulfilling(EntitySpecification<T> specification) {
    return specification.selectSatisfyingElementsFrom(getParts());
  }

  default boolean addPart(T part) {
    if (getOwningClient().isPresent()) {
      checkSameClient(part);
    }
    part.getComposites().add((T) this);
    return getParts().add(part);
  }

  default boolean addParts(Set<T> parts) {
    if (getOwningClient().isPresent()) {
      parts.forEach(CompositeElement.this::checkSameClient);
    }
    var added = false;
    for (var part : parts) {
      if (addPart(part)) {
        added = true;
      }
    }
    return added;
  }

  default boolean removePart(T part) {
    // Parts may be proxies - make sure they are hydrated by calling a method on
    // them.
    if (getParts().removeIf((p) -> p.getId().equals(part.getId()))) {
      part.getComposites().remove(this);
      return true;
    }
    return false;
  }

  default boolean removeParts(Set<T> parts) {
    var removed = false;
    for (var part : new HashSet<>(parts)) {
      if (removePart(part)) {
        removed = true;
      }
    }
    return removed;
  }

  default void setParts(Set<T> parts) {
    if (getOwningClient().isPresent()) {
      parts.stream().forEach(CompositeElement.this::checkSameClient);
    }
    removeParts(getParts());
    addParts(parts);
  }

  default boolean removePartById(Key<UUID> id) {
    return getParts().removeIf(part -> part.getId().equals(id));
  }

  @Override
  default void remove() {
    setParts(new HashSet<>());
    // Work with copies of parent element lists to avoid concurrent modifications
    new HashSet<>(getComposites()).forEach(c -> c.removePart((T) this));
    new HashSet<>(getScopes()).forEach(s -> s.removeMember(this));
  }

  default Set<T> getPartsRecursively() {
    return walkTree(CompositeElement::getParts);
  }

  default Set<T> getCompositesRecursively() {
    return walkTree(CompositeElement::getComposites);
  }

  private Set<T> walkTree(Function<T, Set<T>> getter) {
    Set<T> allElements = new HashSet<>();
    Set<T> elementsOnCurrentLayer = getter.apply((T) this);
    while (!elementsOnCurrentLayer.isEmpty()) {
      allElements.addAll(elementsOnCurrentLayer);
      elementsOnCurrentLayer =
          elementsOnCurrentLayer.stream()
              .flatMap(e -> getter.apply(e).stream())
              .filter(e -> !allElements.contains(e))
              .collect(Collectors.toSet());
    }
    return allElements;
  }
}
