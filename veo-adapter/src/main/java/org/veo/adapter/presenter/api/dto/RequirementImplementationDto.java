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
import org.veo.adapter.presenter.api.common.RequirementImplementationRef;
import org.veo.core.entity.Constraints;
import org.veo.core.entity.Control;
import org.veo.core.entity.Person;
import org.veo.core.entity.RiskAffected;
import org.veo.core.entity.compliance.ImplementationStatus;
import org.veo.core.entity.compliance.Origination;
import org.veo.core.entity.state.RequirementImplementationState;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class RequirementImplementationDto extends AbstractVersionedDto
    implements RequirementImplementationState {
  @JsonIgnore private RequirementImplementationRef selfRef;
  IdRef<RiskAffected<?, ?>> origin;
  IdRef<Control> control;
  IdRef<Person> responsible;
  ImplementationStatus status;

  @Size(min = 1, max = Constraints.DEFAULT_STRING_MAX_LENGTH)
  String implementationStatement;

  Origination origination;

  @JsonProperty(value = "_self", access = JsonProperty.Access.READ_ONLY)
  public String getSelf() {
    return selfRef.getTargetUrl();
  }
}
