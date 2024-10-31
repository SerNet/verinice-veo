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
package org.veo.adapter.service.domaintemplate.dto;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.veo.adapter.presenter.api.dto.AbstractDomainTemplateDto;
import org.veo.adapter.presenter.api.dto.ElementTypeDefinitionDto;
import org.veo.adapter.presenter.api.response.IdentifiableDto;
import org.veo.core.entity.CatalogItem;
import org.veo.core.entity.ControlImplementationConfiguration;
import org.veo.core.entity.DomainBase;
import org.veo.core.entity.IncarnationConfiguration;
import org.veo.core.entity.ProfileState;
import org.veo.core.entity.definitions.DomainMigrationDefinition;
import org.veo.core.entity.inspection.Inspection;
import org.veo.core.entity.state.DomainBaseState;
import org.veo.core.entity.state.ElementTypeDefinitionState;
import org.veo.core.entity.state.TemplateItemState;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

// we accept also dto which conforms to the domain schema, so we need to ignore the additional
// fields it contains
@JsonIgnoreProperties("domainTemplate")
@EqualsAndHashCode(callSuper = true)
@Data
public class ExportDomainTemplateDto extends AbstractDomainTemplateDto
    implements DomainBaseState, IdentifiableDto {

  @ToString.Include private UUID id;

  private Set<ExportCatalogItemDto> catalogItems = new HashSet<>();

  @JsonProperty("profiles_v2")
  private Set<ExportProfileDto> profilesNew = new HashSet<>();

  private Map<String, ElementTypeDefinitionDto> elementTypeDefinitions = new HashMap<>();

  private Map<String, Inspection> inspections = new HashMap<>();

  private IncarnationConfiguration incarnationConfiguration = new IncarnationConfiguration();

  private ControlImplementationConfiguration controlImplementationConfiguration =
      new ControlImplementationConfiguration();

  private DomainMigrationDefinition domainMigrationDefinition =
      new DomainMigrationDefinition(Collections.emptyList());

  @Override
  @JsonIgnore
  public UUID getSelfId() {
    return id;
  }

  @Override
  @JsonIgnore
  public Map<String, ElementTypeDefinitionState> getElementTypeDefinitionStates() {
    return elementTypeDefinitions.entrySet().stream()
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  @Override
  @JsonIgnore
  public Set<TemplateItemState<CatalogItem, DomainBase>> getCatalogItemStates() {
    return new HashSet<>(catalogItems);
  }

  @Override
  @JsonIgnore
  public Set<ProfileState> getProfileStates() {
    return new HashSet<>(profilesNew);
  }
}
