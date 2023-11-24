/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Urs Zeidler.
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

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.util.HashMap;
import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.veo.core.entity.DomainBase;
import org.veo.core.entity.DomainTemplate;
import org.veo.core.entity.Identifiable;
import org.veo.core.entity.Nameable;
import org.veo.core.entity.decision.Decision;
import org.veo.core.entity.riskdefinition.RiskDefinition;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/** Base transfer object for DomainTemplates. Contains common data for all DomainTemplate DTOs. */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@SuppressWarnings("PMD.AbstractClassWithoutAnyMethod")
public abstract class AbstractDomainTemplateDto extends AbstractVersionedSelfReferencingDto
    implements NameableDto {

  @Schema(
      description = "The name for the DomainTemplate.",
      example = "Data protection",
      requiredMode = REQUIRED)
  private String name;

  @Schema(description = "The abbreviation for the DomainTemplate.", example = "DSGVO")
  @Size(max = Nameable.ABBREVIATION_MAX_LENGTH)
  private String abbreviation;

  @Schema(description = "The description for the DomainTemplate.")
  @Size(max = Nameable.DESCRIPTION_MAX_LENGTH)
  private String description;

  @NotNull(message = "A authority must be present.")
  @Schema(
      description = "The authority for the DomainTemplate.",
      example = "ISO",
      requiredMode = REQUIRED)
  @Size(min = 1, max = DomainBase.AUTHORITY_MAX_LENGTH)
  private String authority;

  @NotNull(message = "A templateVersion must be present.")
  @Schema(
      description = "The templateVersion for the DomainTemplate.",
      example = "1.0.0",
      requiredMode = REQUIRED)
  @Size(min = 1, max = DomainTemplate.TEMPLATE_VERSION_MAX_LENGTH)
  private String templateVersion;

  @Schema(description = "A list of risk definitions belonging to the DomainTemplate.")
  private Map<String, RiskDefinition> riskDefinitions = new HashMap<>();

  @Schema(
      description = "The definitions of domain-specific element properties",
      requiredMode = REQUIRED)
  @Valid
  private Map<String, ElementTypeDefinitionDto> elementTypeDefinitions = new HashMap<>();

  private Map<String, Decision> decisions = new HashMap<>();

  @Override
  public Class<? extends Identifiable> getModelInterface() {
    return DomainTemplate.class;
  }
}
