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

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.veo.core.entity.Identifiable;
import org.veo.core.entity.Key;

public interface ITypedId<T extends Identifiable> extends IEntityRef<T> {

  @JsonIgnore
  String getId();

  @JsonIgnore
  Class<T> getType();

  default Key<UUID> toKey() {
    return Key.uuidFrom(getId());
  }

  static <T extends Identifiable> boolean equals(ITypedId<?> a, Object other) {
    return other instanceof ITypedId b
        && a.getId().equals(b.getId())
        && a.getType().equals(b.getType());
  }

  static <T extends Identifiable> int hashCode(ITypedId<?> typedId) {
    return Objects.hash(typedId.getId(), typedId.getType());
  }
}
