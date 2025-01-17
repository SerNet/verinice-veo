/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Jonas Jordan
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
import jakarta.validation.constraints.PositiveOrZero;

import com.fasterxml.jackson.annotation.JsonValue;

import org.veo.core.entity.riskdefinition.CategoryLevel;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/** References an implementation status with a certain key. This should provide some type safety. */
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@EqualsAndHashCode
@Valid
public class ImplementationStatusRef {
  @Getter @PositiveOrZero @JsonValue private int ordinalValue;

  public static ImplementationStatusRef from(CategoryLevel cl) {
    return new ImplementationStatusRef(cl.getOrdinalValue());
  }
}
