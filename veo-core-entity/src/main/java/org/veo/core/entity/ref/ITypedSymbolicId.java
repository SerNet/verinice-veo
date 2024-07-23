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

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;

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
}
