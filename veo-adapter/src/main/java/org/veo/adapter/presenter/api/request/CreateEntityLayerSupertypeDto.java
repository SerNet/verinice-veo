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
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import lombok.Data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

import org.veo.adapter.presenter.api.common.ModelObjectReference;
import org.veo.adapter.presenter.api.response.CustomLinkDto;
import org.veo.adapter.presenter.api.response.CustomPropertiesDto;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Unit;

@Data
public class CreateEntityLayerSupertypeDto {

    @NotNull(message = "A name must be present.")
    @Schema(required = true)
    private String name;

    private String abbreviation;

    private String description;

    private Set<ModelObjectReference<Domain>> domains = Collections.emptySet();

    private Set<CustomLinkDto> links = Collections.emptySet();

    @NotNull(message = "An owner must be present.")
    @Schema(required = true)
    private ModelObjectReference<Unit> owner;

    @Pattern(regexp = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}",
             flags = Pattern.Flag.CASE_INSENSITIVE,
             message = "ID for new objects must either be null or a valid UUID string following RFC 4122.")
    @Schema(description = "ID must be a valid UUID string following RFC 4122.",
            example = "adf037f1-0089-48ad-9177-92269918758b")
    private String id;

    @Schema(description = "A timestamp acc. to RFC 3339 specifying when this version of the entity was saved.",
            example = "1990-12-31T23:59:60Z")
    @Pattern(regexp = "(\\d{4}-\\d{2}-\\d{2}[Tt]\\d{2}:\\d{2}:\\d{2}(\\.\\d{0,2})?([zZ]|[+-]\\d{2}:\\d{2}))")
    private String validFrom;

    @Schema(description = "A timestamp acc. to RFC 3339 specifying the point in time when this version of the entity was superseded "
            + "by a newer version or deleted. Empty if this is the current version.",
            example = "1990-12-31T23:59:60Z")
    @Pattern(regexp = "(\\d{4}-\\d{2}-\\d{2}[Tt]\\d{2}:\\d{2}:\\d{2}(\\.\\d{0,2})?([zZ]|[+-]\\d{2}:\\d{2}))")
    private String validUntil;

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
