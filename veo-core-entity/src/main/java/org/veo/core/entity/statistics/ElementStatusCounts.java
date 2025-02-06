/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Jochen Kemnade
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

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonAnyGetter;

import org.veo.core.entity.Domain;
import org.veo.core.entity.ElementType;

public class ElementStatusCounts {

  private final Map<ElementType, Map<String, Map<String, Long>>> values;

  public ElementStatusCounts(Domain domain) {
    values =
        Stream.of(ElementType.values())
            .collect(
                toMap(
                    identity(),
                    type ->
                        domain.getElementTypeDefinition(type).getSubTypes().entrySet().stream()
                            .collect(
                                toMap(
                                    Entry::getKey,
                                    e ->
                                        new HashMap<>(
                                            e.getValue().getStatuses().stream()
                                                .collect(toMap(identity(), s -> 0L)))))));
  }

  public void setCount(ElementType type, String subType, String status, Long count) {
    Map<String, Long> valuesForSubType = values.get(type).get(subType);
    if (valuesForSubType == null) {
      throw new IllegalArgumentException("Invalid subType: " + subType);
    }
    valuesForSubType.put(status, count);
  }

  @JsonAnyGetter
  public Map<ElementType, Map<String, Map<String, Long>>> getValues() {
    return values;
  }
}
