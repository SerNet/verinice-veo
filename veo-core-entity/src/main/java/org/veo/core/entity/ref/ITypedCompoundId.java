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

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.veo.core.entity.CompoundIdentifiable;
import org.veo.core.entity.EntityType;
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

  @SuppressWarnings("PMD.SuspiciousEqualsMethodName")
  static <T extends Identifiable> boolean equals(ITypedCompoundId<?, ?, ?> a, Object other) {
    return other instanceof ITypedCompoundId b
        && a.getFirstRef().equals(b.getFirstRef())
        && a.getSecondRef().equals(b.getSecondRef())
        && a.getType().equals(b.getType());
  }

  static <T extends Identifiable> int hashCode(ITypedCompoundId<?, ?, ?> typedId) {
    return Objects.hash(typedId.getFirstRef(), typedId.getSecondRef(), typedId.getType());
  }

  static String toString(ITypedCompoundId<?, ?, ?> typedId) {
    return "%s for %s & %s"
        .formatted(
            EntityType.getSingularTermByType(typedId.getType()),
            typedId.getFirstRef(),
            typedId.getSecondRef());
  }
}
