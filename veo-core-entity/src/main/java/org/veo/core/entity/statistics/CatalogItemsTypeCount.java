/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Urs Zeidler
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
package org.veo.core.entity.statistics;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;

import org.veo.core.entity.EntityType;

public class CatalogItemsTypeCount {

  private final Map<String, Map<String, Long>> values = new HashMap<>();

  public void setCount(EntityType type, String subType, Long count) {
    values
        .computeIfAbsent(type.getSingularTerm(), k -> new HashMap<String, Long>())
        .put(subType, count);
  }

  @JsonAnyGetter
  public Map<String, Map<String, Long>> getValues() {
    return values;
  }
}
