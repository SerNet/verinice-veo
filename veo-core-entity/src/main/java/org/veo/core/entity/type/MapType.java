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

import java.util.Optional;

import javax.annotation.Nonnull;

record MapType(VeoType keyType, VeoType valueType) implements VeoType {
  MapType {
    keyType.mustBeIncludedIn(AnythingType.INSTANCE, "map keys cannot be null");
  }

  @Override
  public boolean includes(VeoType other) {
    return other.findMapKeyType().map(keyType::includes).orElse(false)
        && other.findMapValueType().map(valueType::includes).orElse(false);
  }

  @Override
  public boolean intersectsWith(VeoType other) {
    return other.findMapKeyType().map(keyType::intersectsWith).orElse(false)
        && other.findMapValueType().map(valueType::intersectsWith).orElse(false);
  }

  @Override
  public VeoType mustBeMapAndGetKeyType(String errorContext) {
    return keyType;
  }

  @Override
  public VeoType mustBeMapAndGetValueType() {
    return valueType;
  }

  @Override
  public Optional<VeoType> findMapKeyType() {
    return Optional.of(keyType);
  }

  @Override
  public Optional<VeoType> findMapValueType() {
    return Optional.of(valueType);
  }

  @Override
  @Nonnull
  public String toHumanReadable() {
    return "Map<%s,%s>".formatted(keyType, valueType);
  }

  @Override
  @Nonnull
  public String toString() {
    return toHumanReadable();
  }
}
