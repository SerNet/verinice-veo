/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Jonas Jordan
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
package org.veo.adapter.presenter.api.dto;

import static java.util.stream.Collectors.toMap;

import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import org.veo.core.entity.CustomAspect;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;

import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class CustomAspectMapDto {
  @JsonCreator
  public CustomAspectMapDto(Map<String, AttributesDto> value) {
    this.value = value;
  }

  @JsonValue private Map<String, AttributesDto> value = Collections.emptyMap();

  public static CustomAspectMapDto from(Map<String, Map<String, Object>> customAspects) {
    return new CustomAspectMapDto(
        customAspects.entrySet().stream()
            .collect(toMap(Map.Entry::getKey, e -> new AttributesDto(e.getValue()))));
  }

  public static CustomAspectMapDto from(Element source, Domain domain) {
    return new CustomAspectMapDto(
        source.getCustomAspects(domain).stream()
            .collect(toMap(CustomAspect::getType, ca -> new AttributesDto(ca.getAttributes()))));
  }
}
