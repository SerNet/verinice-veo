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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.Valid;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;

import org.veo.core.entity.state.CustomLinkState;

import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
@Valid
public class LinkMapDto {
  @JsonCreator
  public LinkMapDto(Map<String, List<LinkDto>> value) {
    this.value = value;
  }

  @JsonValue private Map<String, List<@Valid LinkDto>> value = Collections.emptyMap();

  @JsonIgnore
  public Set<CustomLinkState> getCustomLinkStates() {
    return value.entrySet().stream()
        .flatMap(
            e ->
                e.getValue().stream()
                    .map(
                        l ->
                            new CustomLinkState.CustomLinkStateImpl(
                                e.getKey(), l.getAttributes().getValue(), l.getTarget())))
        .collect(Collectors.toSet());
  }
}
