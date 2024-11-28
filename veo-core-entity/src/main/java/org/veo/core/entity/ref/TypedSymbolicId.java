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
import org.veo.core.entity.SymIdentifiable;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Value
public class TypedSymbolicId<
        T extends SymIdentifiable<T, TNamespace>, TNamespace extends Identifiable>
    implements ITypedSymbolicId<T, TNamespace> {

  @NonNull UUID symbolicId;
  @NonNull Class<T> type;
  @NonNull ITypedId<TNamespace> ownerRef;

  public static <T extends SymIdentifiable<T, TNamespace>, TNamespace extends Identifiable>
      TypedSymbolicId<T, TNamespace> from(
          UUID symbolicId, Class<T> type, ITypedId<? extends TNamespace> ownerRef) {
    if (symbolicId == null) {
      throw new IllegalArgumentException(
          "Missing symbolic ID for %s in %s"
              .formatted(EntityType.getSingularTermByType(type), ownerRef));
    }

    return new TypedSymbolicId<>(symbolicId, type, (ITypedId<TNamespace>) ownerRef);
  }

  public static <T extends SymIdentifiable<T, TNamespace>, TNamespace extends Identifiable>
      TypedSymbolicId<T, TNamespace> from(T entity) {
    return TypedSymbolicId.from(
        entity.getSymbolicId(),
        (Class<T>) entity.getModelInterface(),
        TypedId.from(entity.getNamespace()));
  }

  @Override
  public UUID getNamespaceId() {
    return ownerRef.getId();
  }

  @Override
  public Class<TNamespace> getNamespaceType() {
    return ownerRef.getType();
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof ITypedSymbolicId<?, ?> b
        && getSymbolicId().equals(b.getSymbolicId())
        && getType().equals(b.getType())
        && getNamespaceId().equals(b.getNamespaceId())
        && getNamespaceType().equals(b.getNamespaceType());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getSymbolicId(), getType(), getNamespaceId(), getNamespaceType());
  }

  @Override
  public String toString() {
    return EntityType.getSingularTermByType(getType())
        + " "
        + getSymbolicId()
        + " in "
        + EntityType.getSingularTermByType(getNamespaceType())
        + " "
        + getNamespaceId();
  }
}
