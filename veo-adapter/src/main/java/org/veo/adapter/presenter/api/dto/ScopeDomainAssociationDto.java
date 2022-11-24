/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Daniel Murygin
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

import org.veo.core.entity.Scope;
import org.veo.core.entity.riskdefinition.RiskDefinition;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** Contains a {@link Scope}'s domain-specific information. */
@Data
@EqualsAndHashCode(callSuper = true)
public class ScopeDomainAssociationDto extends DomainAssociationDto {
  @Schema(description = "The ID of a risk definition in the domain")
  @Size(max = RiskDefinition.MAX_ID_SIZE)
  String riskDefinition;
}
