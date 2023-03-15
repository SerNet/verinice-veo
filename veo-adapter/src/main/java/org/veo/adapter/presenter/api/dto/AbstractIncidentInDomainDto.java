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

import org.veo.core.entity.Identifiable;
import org.veo.core.entity.Incident;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@Schema(
    title = "Incident",
    description =
        "An occurred event that might compromise data security - this DTO represents an incident from the viewpoint of a domain and contains both basic and domain-specific properties.")
public abstract class AbstractIncidentInDomainDto
    extends AbstractCompositeElementInDomainDto<Incident> {

  @Override
  @Schema(description = "Brief description of the incident", example = "Theft of laptops")
  public String getName() {
    return super.getName();
  }

  @Override
  @Schema(description = "Short human-readable identified (not unique)", example = "TOL")
  public String getAbbreviation() {
    return super.getAbbreviation();
  }

  @Override
  @Schema(example = "When unknown persons burgled the main facility, 10 laptops were stolen.")
  public String getDescription() {
    return super.getDescription();
  }

  @Override
  @Schema(description = "Unique human-readable identifier", example = "INC-32")
  public String getDesignator() {
    return super.getDesignator();
  }

  @Override
  public Class<? extends Identifiable> getModelInterface() {
    return Incident.class;
  }
}
