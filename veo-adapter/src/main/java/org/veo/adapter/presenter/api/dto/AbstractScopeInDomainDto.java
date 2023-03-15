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

import static org.veo.core.entity.riskdefinition.RiskDefinition.MAX_ID_SIZE;

import java.util.Collections;
import java.util.Set;

import javax.validation.constraints.Size;

import org.veo.adapter.presenter.api.common.ElementInDomainIdRef;
import org.veo.core.entity.Element;
import org.veo.core.entity.Identifiable;
import org.veo.core.entity.Scope;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@Schema(
    title = "Scope",
    description =
        "A group that can contain different types of elements. Scopes represent organizational structures such as organizations, departments or projects.  This DTO represents a scope from the viewpoint of a domain and contains both basic and domain-specific properties.")
public abstract class AbstractScopeInDomainDto extends AbstractElementInDomainDto<Scope> {

  @Override
  @Schema(example = "Data Inc.")
  public String getName() {
    return super.getName();
  }

  @Override
  @Schema(description = "Short human-readable identifier (not unique)", example = "DT")
  public String getAbbreviation() {
    return super.getAbbreviation();
  }

  @Override
  @Schema(example = "An IT service provider specialized in network security")
  public String getDescription() {
    return super.getDescription();
  }

  @Override
  @Schema(description = "Unique human-readable identifier", example = "SCP-6")
  public String getDesignator() {
    return super.getDesignator();
  }

  @Override
  public Class<? extends Identifiable> getModelInterface() {
    return Scope.class;
  }

  private Set<ElementInDomainIdRef<Element>> members = Collections.emptySet();

  @Schema(description = "The ID of a risk definition")
  @Size(max = MAX_ID_SIZE)
  String riskDefinition;
}
