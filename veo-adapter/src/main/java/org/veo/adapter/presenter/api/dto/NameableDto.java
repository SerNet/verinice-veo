/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2019  Urs Zeidler.
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

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.veo.core.entity.Nameable;

public interface NameableDto extends ModelDto {
  @NotNull(message = "A name must be present.")
  @Size(max = Nameable.NAME_MAX_LENGTH)
  String getName();

  void setName(String aName);

  @Size(max = Nameable.ABBREVIATION_MAX_LENGTH)
  String getAbbreviation();

  void setAbbreviation(String aAbbreviation);

  @Size(max = Nameable.DESCRIPTION_MAX_LENGTH)
  String getDescription();

  void setDescription(String aDescription);
}
