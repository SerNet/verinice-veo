/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Jonas Jordan.
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

import jakarta.validation.constraints.Pattern;

import org.veo.adapter.presenter.api.Patterns;

public class UuidQueryConditionDto extends QueryConditionDto<String> {

  @Override
  @Pattern(regexp = Patterns.UUID, message = "ID must be a valid UUID string following RFC 4122.")
  public Set<String> getValues() {
    return super.getValues();
  }
}
