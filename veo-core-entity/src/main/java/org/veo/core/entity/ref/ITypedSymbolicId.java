/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2024  Jonas Jordan
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

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.veo.core.entity.EntityType;
import org.veo.core.entity.Identifiable;
import org.veo.core.entity.Key;
import org.veo.core.entity.SymIdentifiable;

public interface ITypedSymbolicId<
        T extends SymIdentifiable<T, TNamespace>, TNamespace extends Identifiable>
    extends IEntityRef<T> {
  String getSymbolicId();

  default Key<UUID> getSymbolicKey() {
    return Key.uuidFrom(getSymbolicId());
  }

  default Key<UUID> getOwnerKey() {
    return Key.uuidFrom(getNamespaceId());
  }

  String getNamespaceId();

  Class<TNamespace> getNamespaceType();

  @JsonIgnore
  default ITypedId<TNamespace> getNamespaceRef() {
    return TypedId.from(getNamespaceId(), getNamespaceType());
  }

  @SuppressWarnings("PMD.SuspiciousEqualsMethodName")
  static boolean equals(ITypedSymbolicId<?, ?> a, Object other) {
    return other instanceof ITypedSymbolicId b
        && a.getSymbolicId().equals(b.getSymbolicId())
        && a.getType().equals(b.getType())
        && a.getNamespaceId().equals(b.getNamespaceId())
        && a.getNamespaceType().equals(b.getNamespaceType());
  }

  static int hashCode(ITypedSymbolicId<?, ?> typedId) {
    return Objects.hash(
        typedId.getSymbolicId(),
        typedId.getType(),
        typedId.getNamespaceId(),
        typedId.getNamespaceType());
  }

  static String toString(ITypedSymbolicId<?, ?> typedSymbolicId) {
    return "%s %s in %s %s"
        .formatted(
            EntityType.getSingularTermByType(typedSymbolicId.getType()),
            typedSymbolicId.getSymbolicId(),
            EntityType.getSingularTermByType(typedSymbolicId.getNamespaceType()),
            typedSymbolicId.getNamespaceId());
  }
}
