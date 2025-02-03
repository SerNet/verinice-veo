/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2025  Jochen Kemnade.
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

import org.veo.adapter.presenter.api.dto.full.FullAssetDto;
import org.veo.adapter.presenter.api.dto.full.FullControlDto;
import org.veo.adapter.presenter.api.dto.full.FullDocumentDto;
import org.veo.adapter.presenter.api.dto.full.FullIncidentDto;
import org.veo.adapter.presenter.api.dto.full.FullPersonDto;
import org.veo.adapter.presenter.api.dto.full.FullProcessDto;
import org.veo.adapter.presenter.api.dto.full.FullScenarioDto;
import org.veo.adapter.presenter.api.dto.full.FullScopeDto;
import org.veo.core.entity.Asset;
import org.veo.core.entity.Control;
import org.veo.core.entity.Document;
import org.veo.core.entity.Incident;
import org.veo.core.entity.Person;
import org.veo.core.entity.Process;
import org.veo.core.entity.Scenario;
import org.veo.core.entity.Scope;

import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    discriminatorProperty = "type",
    discriminatorMapping = {
      @DiscriminatorMapping(schema = FullAssetDto.class, value = Asset.SINGULAR_TERM),
      @DiscriminatorMapping(schema = FullControlDto.class, value = Control.SINGULAR_TERM),
      @DiscriminatorMapping(schema = FullDocumentDto.class, value = Document.SINGULAR_TERM),
      @DiscriminatorMapping(schema = FullIncidentDto.class, value = Incident.SINGULAR_TERM),
      @DiscriminatorMapping(schema = FullPersonDto.class, value = Person.SINGULAR_TERM),
      @DiscriminatorMapping(schema = FullProcessDto.class, value = Process.SINGULAR_TERM),
      @DiscriminatorMapping(schema = FullScenarioDto.class, value = Scenario.SINGULAR_TERM),
      @DiscriminatorMapping(schema = FullScopeDto.class, value = Scope.SINGULAR_TERM),
    },
    oneOf = {
      FullAssetDto.class,
      FullControlDto.class,
      FullDocumentDto.class,
      FullIncidentDto.class,
      FullPersonDto.class,
      FullProcessDto.class,
      FullScenarioDto.class,
      FullScopeDto.class
    })
public interface FullElementDto {}
