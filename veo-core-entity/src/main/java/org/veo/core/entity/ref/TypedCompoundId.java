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

import org.veo.core.entity.CompoundIdentifiable;
import org.veo.core.entity.EntityType;
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
  public UUID getFirstId() {
    return firstRef.getId();
  }

  @Override
  public Class<TSecond> getSecondType() {
    return secondRef.getType();
  }

  @Override
  public UUID getSecondId() {
    return secondRef.getId();
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof ITypedCompoundId<?, ?, ?> b
        && getFirstRef().equals(b.getFirstRef())
        && getSecondRef().equals(b.getSecondRef())
        && getType().equals(b.getType());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getFirstRef(), getSecondRef(), getType());
  }

  @Override
  public String toString() {
    return EntityType.getSingularTermByType(getType())
        + " for "
        + getFirstRef()
        + " & "
        + getSecondRef();
  }
}
