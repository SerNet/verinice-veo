/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Urs Zeidler
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
package org.veo.core.entity.profile;

import java.util.Set;

import jakarta.validation.constraints.Size;

import org.veo.core.entity.Constraints;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProfileDefinition {
  static final int NAME_MAX_LENGTH = Constraints.DEFAULT_STRING_MAX_LENGTH;
  static final int DESCRIPTION_MAX_LENGTH = Constraints.DEFAULT_DESCRIPTION_MAX_LENGTH;
  static final int LANGUAGE_MAX_LENGTH = Constraints.DEFAULT_STRING_MAX_LENGTH;

  @Size(max = NAME_MAX_LENGTH)
  private String name;

  @Size(max = DESCRIPTION_MAX_LENGTH)
  private String description;

  @Size(max = LANGUAGE_MAX_LENGTH)
  private String language;

  private Set<?> elements;
  private Set<?> risks;
}
