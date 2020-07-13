/*******************************************************************************
 * Copyright (c) 2020 Jochen Kemnade.
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.veo.adapter.presenter.api.request;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.Valid;

import lombok.Data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

import org.veo.adapter.presenter.api.response.CustomPropertiesDto;

@Data
public class CreateEntityLayerSupertypeDto {
    @JsonIgnore
    @Valid
    private Set<CustomPropertiesDto> customAspects = Collections.emptySet();

    @Schema(description = "A custom property which is determined by the requested entity schema - see '/schemas'",
            name = "customAspects",
            required = false,
            title = "CustomAspect")
    @JsonProperty("customAspects")
    public Map<String, CustomPropertiesDto> getCustomAspectsIntern() {
        Map<String, List<CustomPropertiesDto>> collect = getCustomAspects().stream()
                                                                           .collect(Collectors.groupingBy(cp -> cp.getType()));
        Map<String, CustomPropertiesDto> r = new HashMap<>(collect.size());
        for (Entry<String, List<CustomPropertiesDto>> e : collect.entrySet()) {
            List<CustomPropertiesDto> value = e.getValue();
            if (value.size() != 1) {
                throw new IllegalArgumentException("wrong");
            }
            r.put(e.getKey(), value.get(0));
        }
        return r;
    }

    public void setCustomAspectsIntern(Map<String, CustomPropertiesDto> customAspects) {
        setCustomAspects(customAspects.entrySet()
                                      .stream()
                                      .map(Entry<String, CustomPropertiesDto>::getValue)
                                      .collect(Collectors.toSet()));
    }
}
