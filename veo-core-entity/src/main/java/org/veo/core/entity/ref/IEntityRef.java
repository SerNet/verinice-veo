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

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.veo.core.entity.Entity;
import org.veo.core.entity.Identifiable;
import org.veo.core.entity.SymIdentifiable;
import org.veo.core.entity.exception.UnprocessableDataException;

public interface IEntityRef<T extends Entity> {
  @JsonIgnore
  Class<T> getType();

  static <T extends Entity> IEntityRef<T> from(T entity) {
    if (entity instanceof Identifiable i) {
      return TypedId.from(i.getIdAsString(), (Class) i.getModelInterface());
    } else if (entity instanceof SymIdentifiable s) {
      return TypedSymbolicId.from(s);
    }
    throw new UnprocessableDataException(
        "Unsupported reference type: %s".formatted(entity.getModelInterface()));
  }
}
