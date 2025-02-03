/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Jonas Jordan
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

import java.util.Set;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import org.veo.adapter.presenter.api.dto.full.AssetRiskDto;
import org.veo.adapter.presenter.api.dto.full.FullAssetDto;
import org.veo.adapter.presenter.api.dto.full.FullControlDto;
import org.veo.adapter.presenter.api.dto.full.FullDocumentDto;
import org.veo.adapter.presenter.api.dto.full.FullDomainDto;
import org.veo.adapter.presenter.api.dto.full.FullIncidentDto;
import org.veo.adapter.presenter.api.dto.full.FullPersonDto;
import org.veo.adapter.presenter.api.dto.full.FullProcessDto;
import org.veo.adapter.presenter.api.dto.full.FullScenarioDto;
import org.veo.adapter.presenter.api.dto.full.FullScopeDto;
import org.veo.adapter.presenter.api.dto.full.FullUnitDto;
import org.veo.adapter.presenter.api.dto.full.ProcessRiskDto;
import org.veo.adapter.presenter.api.dto.full.ScopeRiskDto;
import org.veo.core.entity.Asset;
import org.veo.core.entity.Control;
import org.veo.core.entity.Document;
import org.veo.core.entity.Incident;
import org.veo.core.entity.Person;
import org.veo.core.entity.Process;
import org.veo.core.entity.Scenario;
import org.veo.core.entity.Scope;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UnitDumpDto {
  @NotNull(message = "A unit must be present.")
  private @Valid FullUnitDto unit;

  @NotNull(message = "Domain references must be present.")
  private Set<@Valid FullDomainDto> domains;

  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
  @JsonSubTypes({
    @Type(value = FullAssetDto.class, name = Asset.SINGULAR_TERM),
    @Type(value = FullControlDto.class, name = Control.SINGULAR_TERM),
    @Type(value = FullDocumentDto.class, name = Document.SINGULAR_TERM),
    @Type(value = FullIncidentDto.class, name = Incident.SINGULAR_TERM),
    @Type(value = FullPersonDto.class, name = Person.SINGULAR_TERM),
    @Type(value = FullProcessDto.class, name = Process.SINGULAR_TERM),
    @Type(value = FullScenarioDto.class, name = Scenario.SINGULAR_TERM),
    @Type(value = FullScopeDto.class, name = Scope.SINGULAR_TERM),
  })
  @NotNull(message = "Elements must be present.")
  @ArraySchema(schema = @Schema(implementation = FullElementDto.class))
  private Set<@Valid AbstractElementDto> elements;

  @JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
  @JsonSubTypes({
    @Type(value = AssetRiskDto.class),
    @Type(value = ProcessRiskDto.class),
    @Type(value = ScopeRiskDto.class),
  })
  @NotNull(message = "Risks must be present.")
  private Set<@Valid AbstractRiskDto> risks;
}
