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

import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.veo.adapter.presenter.api.common.IdRef;
import org.veo.adapter.presenter.api.common.RequirementImplementationsRef;
import org.veo.core.entity.Constraints;
import org.veo.core.entity.Control;
import org.veo.core.entity.Person;
import org.veo.core.entity.compliance.ImplementationStatus;
import org.veo.core.entity.state.ControlImplementationState;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ControlImplementationDto implements ControlImplementationState {
  IdRef<Control> control;

  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  ImplementationStatus implementationStatus;

  @Schema(description = "Explanation why this control should be implemented on this element")
  @Size(min = 1, max = Constraints.DEFAULT_DESCRIPTION_MAX_LENGTH)
  String description;

  @Schema(
      description =
          "Person who is responsible for whether this control should be implemented on this element")
  IdRef<Person> responsible;

  @JsonIgnore RequirementImplementationsRef requirementImplementationsRef;

  @JsonProperty(value = "_requirementImplementations", access = JsonProperty.Access.READ_ONLY)
  String getRequirementImplementationsUri() {
    return requirementImplementationsRef.getUrl();
  }
}
