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

import java.math.BigDecimal;
import java.util.Comparator;

import javax.annotation.Nonnull;

import org.veo.core.entity.Element;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;

@AllArgsConstructor
@EqualsAndHashCode
sealed class SimpleType implements VeoType permits DurationStringType {
  private Class<?> clazz;
  static final SimpleType STRING = new SimpleType(String.class);
  static final SimpleType INTEGER = new SimpleType(Integer.class);
  static final SimpleType LONG = new SimpleType(Long.class);
  static final SimpleType DECIMAL = new SimpleType(BigDecimal.class);
  static final SimpleType BOOLEAN = new SimpleType(Boolean.class);
  static final SimpleType ELEMENT = new SimpleType(Element.class);

  @Override
  public boolean includes(VeoType other) {
    return other instanceof SimpleType st && clazz.isAssignableFrom(st.clazz);
  }

  @Override
  public <T> Comparator<T> getComparator() {
    if (!Comparable.class.isAssignableFrom(clazz)) {
      throw new IllegalArgumentException("comparison is not supported for %s".formatted(this));
    }
    return (a, b) -> ((Comparable) a).compareTo(b);
  }

  @Override
  @Nonnull
  public String toHumanReadable() {
    return clazz.getSimpleName();
  }

  @Override
  @Nonnull
  public String toString() {
    return toHumanReadable();
  }
}
