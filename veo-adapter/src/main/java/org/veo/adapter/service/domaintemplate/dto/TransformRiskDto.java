/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Jochen Kemnade
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

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

import org.veo.adapter.presenter.api.dto.AbstractRiskDto;
import org.veo.adapter.presenter.api.dto.full.AssetRiskDto;
import org.veo.adapter.presenter.api.dto.full.ProcessRiskDto;
import org.veo.adapter.presenter.api.dto.full.ScopeRiskDto;
import org.veo.core.entity.Element;

import lombok.Data;

/**
 * This DTO represent the contained {@link Element} defined by a FullXXXDto. It uses the 'type'
 * property in the json to determine the actual type.
 */
@Data
@JsonTypeInfo(use = Id.DEDUCTION)
@JsonSubTypes({
  @JsonSubTypes.Type(value = ProcessRiskDto.class, name = "processrisk"),
  @JsonSubTypes.Type(value = ScopeRiskDto.class, name = "scoperisk"),
  @JsonSubTypes.Type(value = AssetRiskDto.class, name = "assetrisk")
})
public class TransformRiskDto extends AbstractRiskDto {}
