/*
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
 */
package org.veo.core.entity.type;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import jakarta.validation.constraints.NotNull;

import org.veo.core.entity.ElementType;
import org.veo.core.entity.definitions.CustomAspectDefinition;

/**
 * Represents data types at runtime. Types are described in terms of set theory, i.e. each type
 * covers a set of possible values, and these sets can include each other or intersect with one
 * another.
 *
 * <p>For our purposes, this interface is more powerful than {@link java.lang.Class}, because it
 * can:
 *
 * <ul>
 *   <li>differentiate between nullable and non-nullable types
 *   <li>represent sum types
 *   <li>represent generic lists and maps and hold the nested types at runtime
 * </ul>
 */
public sealed interface VeoType
    permits AnythingType,
        AttributeContainerType,
        ListType,
        MapType,
        NothingType,
        SimpleType,
        SumType {

  /**
   * @throws IllegalArgumentException if a value could match the other type but not this type
   */
  default void mustInclude(VeoType other, String errorContext) {
    if (!includes(other)) {
      throw new IllegalArgumentException(
          errorContext + ": expected %s, got %s".formatted(other, this));
    }
  }

  /**
   * @throws IllegalArgumentException if a value could match this type but not the other type
   */
  default void mustBeIncludedIn(VeoType other, String errorContext) {
    if (!other.includes(this)) {
      throw new IllegalArgumentException(
          "%s: expected %s, got %s".formatted(errorContext, other, this));
    }
  }

  /**
   * @throws IllegalArgumentException if no value could match both this type and the other type
   */
  default void mustIntersectWith(VeoType other, String errorContext) {
    if (!intersectsWith(other)) {
      throw new IllegalArgumentException(
          "%s: %s does not intersect with %s".formatted(errorContext, this, other));
    }
  }

  static VeoType attributeContainer(CustomAspectDefinition definition) {
    return new AttributeContainerType(definition);
  }

  static VeoType attributeContainer(Map<String, VeoType> attributeTypes) {
    return new AttributeContainerType(attributeTypes);
  }

  /**
   * @return {@code true} if both types share a set of valid values
   */
  default boolean intersectsWith(VeoType other) {
    return includes(other) || other.includes(this);
  }

  /**
   * @return the item type of this list, or {@code Optional.empty} if this is no list.
   */
  default Optional<VeoType> findListItemType() {
    return Optional.empty();
  }

  /**
   * @return the key type of this map, or {@code Optional.empty} if this is no map.
   */
  default Optional<VeoType> findMapKeyType() {
    return Optional.empty();
  }

  /**
   * @return the value type of this map, or {@code Optional.empty} if this is no map.
   */
  default Optional<VeoType> findMapValueType() {
    return Optional.empty();
  }

  /**
   * @throws IllegalArgumentException if this is not a map type
   * @return the key type for this map type
   */
  default VeoType mustBeMapAndGetKeyType(String errorContext) {
    throw new IllegalArgumentException("%s: expected Map, got %s".formatted(errorContext, this));
  }

  /**
   * @throws IllegalArgumentException if this is not a map type
   * @return the value type for this map type
   */
  default VeoType mustBeMapAndGetValueType() {
    throw new IllegalArgumentException("expected Map, got %s".formatted(this));
  }

  default VeoType getAttributeType(@NotNull String attribute, String errorContext) {
    throw new IllegalArgumentException(
        "%s: %s is not an attribute container".formatted(errorContext, this));
  }

  String toHumanReadable();

  default String format(Object value, Locale locale) {
    throw new UnsupportedOperationException("Formatting not supported for %s".formatted(this));
  }

  /**
   * @return {@code true} if all values that match the other type also match this type
   */
  boolean includes(VeoType other);

  default VeoType orNothing() {
    return sumOf(this, NothingType.INSTANCE);
  }

  static VeoType nothing() {
    return NothingType.INSTANCE;
  }

  static VeoType string() {
    return SimpleType.STRING;
  }

  static VeoType number() {
    return sumOf(integer(), looong(), decimal());
  }

  static VeoType integer() {
    return SimpleType.INTEGER;
  }

  static VeoType looong() {
    return SimpleType.LONG;
  }

  static VeoType decimal() {
    return SimpleType.DECIMAL;
  }

  static VeoType bool() {
    return SimpleType.BOOLEAN;
  }

  static VeoType durationString() {
    return DurationStringType.INSTANCE;
  }

  static VeoType listOf(VeoType itemType) {
    return new ListType(itemType);
  }

  static VeoType mapOf(VeoType keyType, VeoType valueType) {
    return new MapType(keyType, valueType);
  }

  static VeoType primitive() {
    return sumOf(string(), number(), bool(), nothing());
  }

  default void mustBeListOrNothing(String errorContext) {
    mustBeIncludedIn(sumOf(listOf(anything().orNothing()).orNothing()), errorContext);
  }

  default VeoType mustBeListAndGetValueType(String errorContext) {
    mustBeIncludedIn(listOf(anything().orNothing()), errorContext);
    return findListItemType().get();
  }

  default <T> Comparator<T> getComparator() {
    throw new IllegalArgumentException("comparison is not supported for %s".formatted(this));
  }

  static VeoType sumOf(VeoType... possibleTypes) {
    return sumOf(List.of(possibleTypes));
  }

  /**
   * @return a type describing values that can match any of the given types
   */
  static VeoType sumOf(Collection<VeoType> possibleTypes) {
    return new SumType(possibleTypes);
  }

  private static VeoType anything() {
    return AnythingType.INSTANCE;
  }

  static VeoType element() {
    return SimpleType.ELEMENT;
  }

  static VeoType element(ElementType elementType) {
    return new SimpleType(elementType.getType());
  }

  static VeoType riskAffected() {
    return SumType.RISK_AFFECTED;
  }

  static VeoType fromValue(Object value) {
    return switch (value) {
      case null -> NothingType.INSTANCE;
      case List<?> items -> listOf(sumOf(items.stream().map(VeoType::fromValue).toList()));
      case Map<?, ?> map ->
          mapOf(
              sumOf(map.keySet().stream().map(VeoType::fromValue).toList()),
              sumOf(map.values().stream().map(VeoType::fromValue).toList()));
      default -> new SimpleType(value.getClass());
    };
  }
}
