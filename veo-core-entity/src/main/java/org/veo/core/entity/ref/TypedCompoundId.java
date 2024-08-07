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

import org.veo.core.entity.CompoundIdentifiable;
import org.veo.core.entity.Identifiable;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Value
public class TypedCompoundId<
        T extends CompoundIdentifiable<TFirst, TSecond>,
        TFirst extends Identifiable,
        TSecond extends Identifiable>
    implements ITypedCompoundId<T, TFirst, TSecond> {

  @NonNull Class<T> type;
  @NonNull ITypedId<TFirst> firstRef;
  @NonNull ITypedId<TSecond> secondRef;

  public static <
          T extends CompoundIdentifiable<TFirst, TSecond>,
          TFirst extends Identifiable,
          TSecond extends Identifiable>
      TypedCompoundId<T, TFirst, TSecond> from(
          Class<T> type,
          ITypedId<? extends TFirst> firstRef,
          ITypedId<? extends TSecond> secondRef) {
    return new TypedCompoundId<>(type, (ITypedId<TFirst>) firstRef, (ITypedId<TSecond>) secondRef);
  }

  public static <
          T extends CompoundIdentifiable<TFirst, TSecond>,
          TFirst extends Identifiable,
          TSecond extends Identifiable>
      TypedCompoundId<T, TFirst, TSecond> from(T entity) {
    return TypedCompoundId.from(
        (Class<T>) entity.getModelInterface(),
        TypedId.from(entity.getFirstRelation()),
        TypedId.from(entity.getSecondRelation()));
  }

  @Override
  public Class<TFirst> getFirstType() {
    return firstRef.getType();
  }

  @Override
  public String getFirstId() {
    return firstRef.getId();
  }

  @Override
  public Class<TSecond> getSecondType() {
    return secondRef.getType();
  }

  @Override
  public String getSecondId() {
    return secondRef.getId();
  }

  @Override
  public boolean equals(Object other) {
    return ITypedCompoundId.equals(this, other);
  }

  @Override
  public int hashCode() {
    return ITypedCompoundId.hashCode(this);
  }

  @Override
  public String toString() {
    return ITypedCompoundId.toString(this);
  }
}
