/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2025  Alina Tsikunova
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

import java.util.UUID;

import org.veo.core.entity.ElementType;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Schema(accessMode = Schema.AccessMode.READ_ONLY)
@Data
@Setter(AccessLevel.NONE)
@RequiredArgsConstructor
public class GraphNodeDto {
  private final String id;
  private final String displayName;
  private final ElementType elementType;
  private final UUID elementId;
}
