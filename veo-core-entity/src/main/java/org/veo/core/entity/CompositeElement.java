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

import org.veo.core.entity.specification.EntitySpecification;

/**
 * An element whose parts can optionally be modeled as well
 *
 * @param <T>
 *            the type of the entity and its parts
 */
public interface CompositeElement<T extends CompositeElement> extends Element {

    Set<T> getParts();

    Set<T> getComposites();

    default Set<T> findPartsFulfilling(EntitySpecification<T> specification) {
        return specification.selectSatisfyingElementsFrom(getParts());
    }

    default boolean addPart(T part) {
        if (getOwningClient().isPresent()) {
            checkSameClient(part);
        }
        part.getComposites()
            .add(this);
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
        if (getParts().remove(part)) {
            part.getComposites()
                .remove(this);
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
            parts.stream()
                 .forEach(CompositeElement.this::checkSameClient);
        }
        removeParts(getParts());
        addParts(parts);
    }

    default boolean removePartById(Key<UUID> id) {
        return getParts().removeIf(part -> part.getId()
                                               .equals(id));
    }

    @Override
    default void remove() {
        setParts(new HashSet<>());
        // Work with copies of parent element lists to avoid concurrent modifications
        new HashSet<>(getComposites()).forEach(c -> c.removePart(this));
        new HashSet<>(getScopes()).forEach(s -> s.removeMember(this));
    }
}
