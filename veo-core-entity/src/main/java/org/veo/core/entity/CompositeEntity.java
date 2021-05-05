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

import java.util.Set;
import java.util.UUID;

import org.veo.core.entity.specification.EntitySpecification;

/**
 * An entity whose parts can optionally be modeled as well
 *
 * @param <T>
 *            the type of the entity and its parts
 */
public interface CompositeEntity<T extends EntityLayerSupertype> extends EntityLayerSupertype {

    Set<T> getParts();

    default Set<T> findPartsFulfilling(EntitySpecification<T> specification) {
        return specification.selectSatisfyingElementsFrom(getParts());
    }

    default boolean addPart(T part) {
        checkSameClient(part.getOwner()
                            .getClient());
        return getParts().add(part);
    }

    default boolean addParts(Set<T> parts) {
        parts.forEach(part -> checkSameClient(part.getOwner()
                                                  .getClient()));
        return getParts().addAll(parts);
    }

    default boolean removePart(T part) {
        return getParts().remove(part);
    }

    default boolean removeParts(Set<T> parts) {
        return getParts().removeAll(parts);
    }

    default void setParts(Set<T> parts) {
        parts.stream()
             .forEach(part -> checkSameClient(part.getOwner()
                                                  .getClient()));
        getParts().clear();
        getParts().addAll(parts);
    }

    default boolean removePartById(Key<UUID> id) {
        return getParts().removeIf(part -> part.getId()
                                               .equals(id));
    }

}
