/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Urs Zeidler
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

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.veo.adapter.presenter.api.dto.AbstractProfileDto;
import org.veo.adapter.presenter.api.response.IdentifiableDto;
import org.veo.core.entity.ProfileState;
import org.veo.core.entity.state.ProfileItemState;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
public class ExportProfileDto extends AbstractProfileDto implements IdentifiableDto, ProfileState {

  @ToString.Include private UUID id;

  @Schema(description = "The profile-items for the Profile.")
  private Set<ExportProfileItemDto> items = new HashSet<>();

  @Override
  @JsonIgnore
  public Set<ProfileItemState> getItemStates() {
    return items.stream().map(pi -> (ProfileItemState) pi).collect(Collectors.toSet());
  }
}
