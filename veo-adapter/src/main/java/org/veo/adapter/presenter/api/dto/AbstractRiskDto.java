/*******************************************************************************
 * Copyright (c) 2020 Alexander Koderman.
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

import java.util.Collections;
import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.veo.adapter.presenter.api.common.ModelObjectReference;
import org.veo.adapter.presenter.api.openapi.ModelObjectReferenceDomains;
import org.veo.adapter.presenter.api.openapi.ModelObjectReferenceOwner;
import org.veo.core.entity.Control;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Person;
import org.veo.core.entity.Scenario;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Singular;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@Valid
@SuppressWarnings("PMD.AbstractClassWithoutAnyMethod")
public abstract class AbstractRiskDto extends AbstractVersionedDto {

    @Valid
    @ArraySchema(schema = @Schema(implementation = ModelObjectReferenceDomains.class))
    @Singular
    private Set<ModelObjectReference<Domain>> domains = Collections.emptySet();

    @Valid
    @NotNull(message = "A scenario must be present.")
    @Schema(required = true, implementation = ModelObjectReferenceOwner.class)
    private ModelObjectReference<Scenario> scenario;

    @Valid
    @Schema(implementation = ModelObjectReferenceOwner.class,
            description = "This risk is mitigated by this control or control-composite.")
    private ModelObjectReference<Control> mitigation;

    @Valid
    @Schema(implementation = ModelObjectReferenceOwner.class,
            description = "The accountable point-of-contact for this risk.")
    private ModelObjectReference<Person> riskOwner;

}
