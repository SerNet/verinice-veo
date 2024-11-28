/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Jochen Kemnade
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
package org.veo.core.entity.ref;

import java.util.Objects;
import java.util.UUID;

import org.veo.core.entity.EntityType;
import org.veo.core.entity.Identifiable;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Value
public class TypedId<T extends Identifiable> implements ITypedId<T> {

  @NonNull private final UUID id;

  @NonNull private final Class<T> type;

  public static <T extends Identifiable> TypedId<T> from(UUID id, Class<T> type) {
    if (id == null) {
      throw new IllegalArgumentException(
          "Missing ID for %s".formatted(EntityType.getSingularTermByType(type)));
    }
    return new TypedId<>(id, type);
  }

  public static <T extends Identifiable> TypedId<T> from(T entity) {
    return TypedId.from(entity.getId(), (Class<T>) entity.getModelInterface());
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof ITypedId<?> b
        && getId().equals(b.getId())
        && getType().equals(b.getType());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getId(), getType());
  }

  @Override
  public String toString() {
    return EntityType.getSingularTermByType(getType()) + " " + getId();
  }
}
