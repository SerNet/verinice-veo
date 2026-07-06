/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2026  Jonas Jordan
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
package org.veo.core.entity.type;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.veo.core.entity.ElementType;

import edu.umd.cs.findbugs.annotations.NonNull;

record SumType(@Nonnull Set<VeoType> possibleTypes) implements VeoType {
  static final SumType RISK_AFFECTED =
      new SumType(
          ElementType.RISK_AFFECTED_TYPES.stream()
              .map(VeoType::element)
              .collect(Collectors.toSet()));

  SumType {
    if (possibleTypes.isEmpty()) {
      throw new IllegalArgumentException("Sum type cannot be empty");
    }
    // avoid nested sum types
    var flatTypes =
        possibleTypes.stream().map(SumType::unpackTypes).flatMap(Collection::stream).toList();
    // remove redundant types (that are already covered by other types)
    possibleTypes =
        flatTypes.stream()
            .filter(t -> flatTypes.stream().noneMatch(o -> !o.equals(t) && o.includes(t)))
            .distinct()
            .collect(Collectors.toSet());
  }

  @Override
  public boolean includes(VeoType other) {
    return unpackTypes(other).stream()
        .allMatch(otherType -> possibleTypes.stream().anyMatch(p -> p.includes(otherType)));
  }

  @Override
  public String format(Object value, Locale locale) {
    if (value == null) {
      return VeoType.nothing().format(null, locale);
    }
    var nonNullTypes = possibleTypes.stream().filter(e -> !(e instanceof NothingType)).toList();
    if (nonNullTypes.size() > 1) {
      throw new IllegalArgumentException(
          "cannot format values with different types (%s)".formatted(this));
    }
    return nonNullTypes.getFirst().format(value, locale);
  }

  @Override
  public VeoType getAttributeType(String attribute, String errorContext) {
    return VeoType.sumOf(
        possibleTypes.stream()
            .map(
                p ->
                    p.getAttributeType(
                        attribute, "%s: %s does not match".formatted(errorContext, this)))
            .toList());
  }

  @Override
  public <T> Comparator<T> getComparator() {
    var nonNullTypes = possibleTypes.stream().filter(e -> !(e instanceof NothingType)).toList();
    if (nonNullTypes.isEmpty()) {
      return (_, _) -> 0;
    }
    if (nonNullTypes.size() > 1) {
      throw new IllegalArgumentException(
          "cannot compare values with different types (%s)".formatted(this));
    }
    return Comparator.nullsLast(nonNullTypes.getFirst().getComparator());
  }

  @Override
  public boolean intersectsWith(VeoType other) {
    return unpackTypes(other).stream()
        .anyMatch(otherType -> possibleTypes.stream().anyMatch(p -> p.intersectsWith(otherType)));
  }

  @Override
  public Optional<VeoType> findListItemType() {
    return flatMap(VeoType::findListItemType);
  }

  @Override
  public Optional<VeoType> findMapKeyType() {
    return flatMap(VeoType::findMapKeyType);
  }

  @Override
  public Optional<VeoType> findMapValueType() {
    return flatMap(VeoType::findMapValueType);
  }

  @Override
  @Nonnull
  public String toHumanReadable() {
    return possibleTypes.stream()
        .map(VeoType::toHumanReadable)
        .sorted() // makes order deterministic
        .sorted(Comparator.comparing("Null"::equals))
        .collect(Collectors.joining("|"));
  }

  @Override
  @Nonnull
  public String toString() {
    return toHumanReadable();
  }

  @NonNull
  private Optional<VeoType> flatMap(Function<VeoType, Optional<VeoType>> mapper) {
    List<VeoType> types =
        possibleTypes.stream().map(mapper).filter(Optional::isPresent).map(Optional::get).toList();
    if (types.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(VeoType.sumOf(types));
  }

  @NonNull
  private static Collection<VeoType> unpackTypes(VeoType type) {
    return type instanceof SumType(Collection<VeoType> types) ? types : List.of(type);
  }
}
