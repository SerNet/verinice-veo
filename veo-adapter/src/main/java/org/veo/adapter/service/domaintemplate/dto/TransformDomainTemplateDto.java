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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import org.veo.adapter.presenter.api.Patterns;
import org.veo.adapter.presenter.api.dto.AbstractCatalogDto;
import org.veo.adapter.presenter.api.dto.AbstractDomainTemplateDto;
import org.veo.adapter.presenter.api.dto.AbstractElementDto;
import org.veo.adapter.presenter.api.dto.AbstractRiskDto;
import org.veo.adapter.presenter.api.dto.ElementTypeDefinitionDto;
import org.veo.adapter.presenter.api.dto.full.AssetRiskDto;
import org.veo.adapter.presenter.api.dto.full.FullAssetDto;
import org.veo.adapter.presenter.api.dto.full.FullControlDto;
import org.veo.adapter.presenter.api.dto.full.FullDocumentDto;
import org.veo.adapter.presenter.api.dto.full.FullIncidentDto;
import org.veo.adapter.presenter.api.dto.full.FullPersonDto;
import org.veo.adapter.presenter.api.dto.full.FullProcessDto;
import org.veo.adapter.presenter.api.dto.full.FullScenarioDto;
import org.veo.adapter.presenter.api.dto.full.FullScopeDto;
import org.veo.adapter.presenter.api.dto.full.ProcessRiskDto;
import org.veo.adapter.presenter.api.dto.full.ScopeRiskDto;
import org.veo.adapter.presenter.api.response.IdentifiableDto;
import org.veo.core.entity.Asset;
import org.veo.core.entity.Control;
import org.veo.core.entity.Document;
import org.veo.core.entity.Incident;
import org.veo.core.entity.Person;
import org.veo.core.entity.Process;
import org.veo.core.entity.Scenario;
import org.veo.core.entity.Scope;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@Data
public class TransformDomainTemplateDto extends AbstractDomainTemplateDto
    implements IdentifiableDto {

  @Pattern(regexp = Patterns.UUID, message = "ID must be a valid UUID string following RFC 4122.")
  @Schema(
      description =
          "ID must be a valid UUID string following RFC 4122. "
              + " Offical templates should have a name base uuid defined in sec. 4.3",
      example = "f8ed22b1-b277-56ec-a2ce-0dbd94e24824",
      format = "uuid")
  @ToString.Include
  private String id;

  @JsonTypeInfo(
      use = JsonTypeInfo.Id.NAME,
      include = JsonTypeInfo.As.EXISTING_PROPERTY,
      property = "type")
  @JsonSubTypes({
    @JsonSubTypes.Type(value = FullAssetDto.class, name = Asset.SINGULAR_TERM),
    @JsonSubTypes.Type(value = FullControlDto.class, name = Control.SINGULAR_TERM),
    @JsonSubTypes.Type(value = FullDocumentDto.class, name = Document.SINGULAR_TERM),
    @JsonSubTypes.Type(value = FullIncidentDto.class, name = Incident.SINGULAR_TERM),
    @JsonSubTypes.Type(value = FullPersonDto.class, name = Person.SINGULAR_TERM),
    @JsonSubTypes.Type(value = FullProcessDto.class, name = Process.SINGULAR_TERM),
    @JsonSubTypes.Type(value = FullScenarioDto.class, name = Scenario.SINGULAR_TERM),
    @JsonSubTypes.Type(value = FullScopeDto.class, name = Scope.SINGULAR_TERM)
  })
  private Set<AbstractElementDto> demoUnitElements = new HashSet<>();

  @JsonTypeInfo(use = Id.DEDUCTION)
  @JsonSubTypes({
    @JsonSubTypes.Type(value = ProcessRiskDto.class, name = "processrisk"),
    @JsonSubTypes.Type(value = ScopeRiskDto.class, name = "scoperisk"),
    @JsonSubTypes.Type(value = AssetRiskDto.class, name = "assetrisk")
  })
  private Set<AbstractRiskDto> demoUnitRisks = new HashSet<>();

  @JsonDeserialize(contentAs = TransformCatalogDto.class)
  @Override
  public void setCatalogs(Set<AbstractCatalogDto> catalogs) {
    super.setCatalogs(catalogs);
  }

  private Map<String, ElementTypeDefinitionDto> elementTypeDefinitions = new HashMap<>();
}
