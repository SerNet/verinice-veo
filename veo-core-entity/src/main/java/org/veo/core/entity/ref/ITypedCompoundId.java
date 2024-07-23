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

import org.veo.core.entity.CompoundIdentifiable;
import org.veo.core.entity.Identifiable;

public interface ITypedCompoundId<
        T extends CompoundIdentifiable<TFirst, TSecond>,
        TFirst extends Identifiable,
        TSecond extends Identifiable>
    extends IEntityRef<T> {

  @JsonIgnore
  Class<T> getType();

  @JsonIgnore
  Class<TFirst> getFirstType();

  @JsonIgnore
  String getFirstId();

  @JsonIgnore
  default ITypedId<TFirst> getFirstRef() {
    return TypedId.from(getFirstId(), getFirstType());
  }

  @JsonIgnore
  Class<TSecond> getSecondType();

  @JsonIgnore
  String getSecondId();

  @JsonIgnore
  default ITypedId<TSecond> getSecondRef() {
    return TypedId.from(getSecondId(), getSecondType());
  }
}
