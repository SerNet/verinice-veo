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

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;

import org.veo.core.entity.ElementType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    description = "Amount of catalog items per sub type, grouped by element type",
    example = "{\"control\": {\"TOM\": 8}, \"scenario\": {\"Attack\": 13, \"NaturalDisaster\": 5}}",
    accessMode = Schema.AccessMode.READ_ONLY)
public class CatalogItemsTypeCount {

  private final Map<ElementType, Map<String, Long>> values = new EnumMap<>(ElementType.class);

  public void setCount(ElementType type, String subType, Long count) {
    values.computeIfAbsent(type, k -> new HashMap<String, Long>()).put(subType, count);
  }

  @JsonAnyGetter
  public Map<ElementType, Map<String, Long>> getValues() {
    return values;
  }
}
