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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.veo.core.entity.DomainTemplate;
import org.veo.core.entity.Identifiable;
import org.veo.core.entity.Nameable;
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

  @NotNull(message = "A name must be present.")
  @Schema(
      description = "The name for the DomainTemplate.",
      example = "Data protection",
      required = true)
  @Size(max = Nameable.NAME_MAX_LENGTH)
  private String name;

  @Schema(description = "The abbreviation for the DomainTemplate.", example = "DSGVO")
  @Size(max = Nameable.ABBREVIATION_MAX_LENGTH)
  private String abbreviation;

  @Schema(description = "The description for the DomainTemplate.")
  @Size(max = Nameable.DESCRIPTION_MAX_LENGTH)
  private String description;

  @NotNull(message = "A authority must be present.")
  @Schema(description = "The authority for the DomainTemplate.", example = "ISO", required = true)
  @Size(min = 1, max = DomainTemplate.AUTHORITY_MAX_LENGTH)
  private String authority;

  @NotNull(message = "A templateVersion must be present.")
  @Schema(
      description = "The templateVersion for the DomainTemplate.",
      example = "1.0",
      required = true)
  @Size(min = 1, max = DomainTemplate.TEMPLATE_VERSION_MAX_LENGTH)
  private String templateVersion;

  @NotNull(message = "A revision must be present.")
  @Schema(description = "The revision for the DomainTemplate.", example = "0", required = true)
  @Size(min = 1, max = DomainTemplate.REVISION_MAX_LENGTH)
  private String revision;

  @Schema(description = "A list of catalogs belonging to the DomainTemplate.")
  private Set<AbstractCatalogDto> catalogs;

  @Schema(description = "A list of risk definitions belonging to the DomainTemplate.")
  private Map<String, RiskDefinition> riskDefinitions = new HashMap<>();

  @Override
  public Class<? extends Identifiable> getModelInterface() {
    return DomainTemplate.class;
  }
}
