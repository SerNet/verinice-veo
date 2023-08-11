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
package org.veo.adapter.presenter.api;

import java.util.Arrays;

import org.veo.adapter.presenter.api.dto.AbstractElementDto;
import org.veo.adapter.presenter.api.dto.AbstractElementInDomainDto;
import org.veo.adapter.presenter.api.dto.AssetDomainAssociationDto;
import org.veo.adapter.presenter.api.dto.ControlDomainAssociationDto;
import org.veo.adapter.presenter.api.dto.DomainAssociationDto;
import org.veo.adapter.presenter.api.dto.ProcessDomainAssociationDto;
import org.veo.adapter.presenter.api.dto.ScenarioDomainAssociationDto;
import org.veo.adapter.presenter.api.dto.ScopeDomainAssociationDto;
import org.veo.adapter.presenter.api.dto.full.FullAssetDto;
import org.veo.adapter.presenter.api.dto.full.FullAssetInDomainDto;
import org.veo.adapter.presenter.api.dto.full.FullControlDto;
import org.veo.adapter.presenter.api.dto.full.FullControlInDomainDto;
import org.veo.adapter.presenter.api.dto.full.FullDocumentDto;
import org.veo.adapter.presenter.api.dto.full.FullDocumentInDomainDto;
import org.veo.adapter.presenter.api.dto.full.FullIncidentDto;
import org.veo.adapter.presenter.api.dto.full.FullIncidentInDomainDto;
import org.veo.adapter.presenter.api.dto.full.FullPersonDto;
import org.veo.adapter.presenter.api.dto.full.FullPersonInDomainDto;
import org.veo.adapter.presenter.api.dto.full.FullProcessDto;
import org.veo.adapter.presenter.api.dto.full.FullProcessInDomainDto;
import org.veo.adapter.presenter.api.dto.full.FullScenarioDto;
import org.veo.adapter.presenter.api.dto.full.FullScenarioInDomainDto;
import org.veo.adapter.presenter.api.dto.full.FullScopeDto;
import org.veo.adapter.presenter.api.dto.full.FullScopeInDomainDto;
import org.veo.core.entity.EntityType;
import org.veo.core.entity.exception.NotFoundException;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum ElementTypeDtoInfo {
  ASSET(
      EntityType.ASSET,
      FullAssetDto.class,
      FullAssetInDomainDto.class,
      AssetDomainAssociationDto.class),
  CONTROL(
      EntityType.CONTROL,
      FullControlDto.class,
      FullControlInDomainDto.class,
      ControlDomainAssociationDto.class),
  DOCUMENT(
      EntityType.DOCUMENT,
      FullDocumentDto.class,
      FullDocumentInDomainDto.class,
      DomainAssociationDto.class),
  INCIDENT(
      EntityType.INCIDENT,
      FullIncidentDto.class,
      FullIncidentInDomainDto.class,
      DomainAssociationDto.class),
  PERSON(
      EntityType.PERSON,
      FullPersonDto.class,
      FullPersonInDomainDto.class,
      DomainAssociationDto.class),
  PROCESS(
      EntityType.PROCESS,
      FullProcessDto.class,
      FullProcessInDomainDto.class,
      ProcessDomainAssociationDto.class),
  SCENARIO(
      EntityType.SCENARIO,
      FullScenarioDto.class,
      FullScenarioInDomainDto.class,
      ScenarioDomainAssociationDto.class),
  SCOPE(
      EntityType.SCOPE,
      FullScopeDto.class,
      FullScopeInDomainDto.class,
      ScopeDomainAssociationDto.class),
  ;

  @Getter private final EntityType entityType;
  @Getter private final Class<? extends AbstractElementDto> fullDtoClass;
  @Getter private final Class<? extends AbstractElementInDomainDto> fullDomainSpecificDtoClass;
  @Getter private final Class<? extends DomainAssociationDto> domainAssociationDtoClass;

  public String getSingularTerm() {
    return entityType.getSingularTerm();
  }

  public static ElementTypeDtoInfo get(EntityType entityType) {
    return Arrays.stream(values())
        .filter(et -> et.getEntityType().equals(entityType))
        .findFirst()
        .orElseThrow(
            () ->
                new NotFoundException(
                    "element type %s not found".formatted(entityType.getSingularTerm())));
  }
}
