/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Jochen Kemnade.
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
package org.veo.adapter.presenter.api.dto.full;

import static com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY;

import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.veo.adapter.presenter.api.dto.AbstractScopeDto;
import org.veo.adapter.presenter.api.response.IdentifiableDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class FullScopeDto extends AbstractScopeDto implements IdentifiableDto {

  private UUID id;

  @JsonProperty(access = READ_ONLY)
  @Schema(description = "An optional map of all risks and risk-values for this scope.")
  private Set<ScopeRiskDto> risks;
}
