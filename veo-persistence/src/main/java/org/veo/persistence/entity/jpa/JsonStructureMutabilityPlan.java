/*
 * verinice.veo
 * Copyright (C) 2026  Jochen Kemnade.
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
package org.veo.persistence.entity.jpa;

import java.io.Serial;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.type.descriptor.java.MutableMutabilityPlan;

/**
 * Hibernate keeps a copy of every loaded mutable value to detect changes later (dirty checking).
 * The default copy of a JSON value is made by writing it to JSON and reading it back, which
 * duplicates every string in it. For large attribute values, that doubles the memory they need for
 * as long as the persistence context lives.
 *
 * <p>This plan copies only the containers (maps and lists), which are the things that can be
 * changed in place, and shares everything else (strings, numbers, booleans), which cannot. Changes
 * to the value are still detected, and a copy never shares a map or a list with the original.
 *
 * <p>That only works for values that consist of nothing but maps with string keys, lists, strings,
 * numbers, booleans and nulls, like parsed JSON. Any other value would be shared even if it can be
 * changed in place, so that its changes would go unnoticed. To keep the plan from being used for
 * attributes with other types, copying a value of another type throws an exception.
 */
public class JsonStructureMutabilityPlan extends MutableMutabilityPlan<Object> {

  @Serial private static final long serialVersionUID = -1626872997961245828L;

  @Override
  protected Object deepCopyNotNull(Object value) {
    return copy(value);
  }

  private static Object copy(Object value) {
    if (value == null
        || value instanceof String
        || value instanceof Number
        || value instanceof Boolean) {
      return value;
    }
    if (value instanceof Map<?, ?> map) {
      var result = new LinkedHashMap<Object, Object>();
      map.forEach((key, element) -> {
        if (!(key instanceof String)) {
          throw unsupported(key);
        }
        result.put(key, copy(element));
      });
      return result;
    }
    if (value instanceof List<?> list) {
      var result = new ArrayList<>(list.size());
      list.forEach(element -> result.add(copy(element)));
      return result;
    }
    throw unsupported(value);
  }

  private static IllegalArgumentException unsupported(Object value) {
    return new IllegalArgumentException(
        "%s cannot copy a value of type %s. It supports only maps with string keys, lists, strings, numbers, booleans and null."
            .formatted(
                JsonStructureMutabilityPlan.class.getSimpleName(), value.getClass().getName()));
  }
}
