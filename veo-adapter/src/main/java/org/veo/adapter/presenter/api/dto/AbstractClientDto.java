/*******************************************************************************
 * Copyright (c) 2019 Urs Zeidler.
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
package org.veo.adapter.presenter.api.dto;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.validation.constraints.Pattern;

import org.veo.adapter.presenter.api.common.ModelObjectReference;
import org.veo.core.entity.ModelObject;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.ToString;

/**
 * Base transfer object for Clients. Contains common data for all Client DTOs.
 */
@Data
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
abstract public class AbstractClientDto implements VersionedDto {

    @Schema(description = "The name for the Client.", example = "SerNet GmbH")
    @ToString.Include
    private String name;

    @Schema(description = "A timestamp acc. to RFC 3339 specifying when this version of the entity was saved.",
            example = "1990-12-31T23:59:60Z")
    @Pattern(regexp = "(\\d{4}-\\d{2}-\\d{2}[Tt]\\d{2}:\\d{2}:\\d{2}(\\.\\d{0,2})?([zZ]|[+-]\\d{2}:\\d{2}))")
    private String validFrom;

    @com.fasterxml.jackson.annotation.JsonIgnore
    private long version;

    @Schema(description = "The units for the Client.")
    private Set<AbstractUnitDto> units = Collections.emptySet();

    @Schema(description = "The domains for the Client.")
    private Set<AbstractDomainDto> domains = Collections.emptySet();

    public Collection<ModelObjectReference<? extends ModelObject>> getReferences() {
        List<ModelObjectReference<? extends ModelObject>> list = new ArrayList<>();
        return list;
    }
}
