/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Alexander Koderman
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
package org.veo.core.entity.risk;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonValue;

import org.veo.core.entity.Constraints;
import org.veo.core.entity.riskdefinition.CategoryDefinition;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@AllArgsConstructor(access = AccessLevel.PACKAGE)
@EqualsAndHashCode
@Valid
@ToString
public class CategoryRef {
  public static final int MAX_ID_LENGTH = Constraints.DEFAULT_CONSTANT_MAX_LENGTH;

  @Getter
  @Size(max = MAX_ID_LENGTH)
  @JsonValue
  String idRef;

  public static CategoryRef from(CategoryDefinition cd) {
    return new CategoryRef(cd.getId());
  }
}
