/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2019  Urs Zeidler.
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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jakarta.validation.constraints.NotNull;

import org.veo.adapter.presenter.api.common.IdRef;
import org.veo.adapter.presenter.api.openapi.IdRefDomainCatalogs;
import org.veo.core.entity.Catalog;
import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainTemplate;
import org.veo.core.entity.Identifiable;
import org.veo.core.entity.decision.Decision;
import org.veo.core.entity.profile.ProfileDefinition;
import org.veo.core.entity.riskdefinition.RiskDefinition;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/** Base transfer object for Domains. Contains common data for all Domain DTOs. */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
public abstract class AbstractDomainDto extends AbstractVersionedSelfReferencingDto
    implements NameableDto {

  @Schema(
      description = "The name for the Domain.",
      example = "Data protection",
      requiredMode = REQUIRED)
  @ToString.Include
  private String name;

  @Schema(description = "The abbreviation for the Domain.", example = "Data prot.")
  private String abbreviation;

  @Schema(
      description = "The description for the Domain.",
      example = "Everything around data protection.")
  private String description;

  @NotNull(message = "An authority must be present.")
  @Schema(
      description = "The orgnization that published a standard",
      example = "ISO",
      requiredMode = REQUIRED,
      accessMode = Schema.AccessMode.READ_ONLY)
  private String authority;

  @NotNull(message = "A templateVersion must be present.")
  @Schema(
      description = "Template version in Semantic Versioning 2.0.0 format",
      example = "1.0.0",
      accessMode = Schema.AccessMode.READ_ONLY)
  private String templateVersion;

  @ArraySchema(schema = @Schema(implementation = IdRefDomainCatalogs.class))
  private Set<IdRef<Catalog>> catalogs = new HashSet<>();

  @Schema(description = "A list of risk definitions belonging to the domain.")
  private Map<String, RiskDefinition> riskDefinitions = new HashMap<>();

  private IdRef<DomainTemplate> domainTemplate;

  private Map<String, Decision> decisions;

  @Schema(
      description = "The profiles that belong to this domain keyed by their symbolic names.",
      requiredMode = REQUIRED)
  private Map<String, ProfileDefinition> profiles = new HashMap<>();

  @Schema(
      description = "The definitions of domain-specific element properties",
      requiredMode = REQUIRED)
  private Map<String, ElementTypeDefinitionDto> elementTypeDefinitions = new HashMap<>();

  @Override
  public Class<? extends Identifiable> getModelInterface() {
    return Domain.class;
  }
}
