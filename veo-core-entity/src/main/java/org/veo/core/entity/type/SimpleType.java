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

import javax.annotation.Nonnull;

import org.veo.core.entity.Element;

record SimpleType(Class<?> clazz) implements VeoType {
  static final SimpleType STRING = new SimpleType(String.class);
  static final SimpleType INTEGER = new SimpleType(Integer.class);
  static final SimpleType LONG = new SimpleType(Long.class);
  static final SimpleType DECIMAL = new SimpleType(BigDecimal.class);
  static final SimpleType BOOLEAN = new SimpleType(Boolean.class);
  static final SimpleType ELEMENT = new SimpleType(Element.class);

  @Override
  public boolean includes(VeoType other) {
    return other instanceof SimpleType(Class<?> otherClazz) && clazz.isAssignableFrom(otherClazz);
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
