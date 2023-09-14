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
package org.veo.core.entity.definitions;

import static org.veo.core.entity.aspects.SubTypeAspect.SUB_TYPE_MAX_LENGTH;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class LinkDefinition extends CustomAspectDefinition {
  @NotNull
  @Size(min = 1, max = 32)
  private String targetType;

  @NotNull
  @Size(min = 1, max = SUB_TYPE_MAX_LENGTH)
  private String targetSubType;
}
