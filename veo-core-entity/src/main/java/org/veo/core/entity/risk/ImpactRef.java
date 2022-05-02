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

import java.math.BigDecimal;

import javax.validation.Valid;
import javax.validation.constraints.PositiveOrZero;

import org.veo.core.entity.riskdefinition.CategoryLevel;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Depending on the category definition, an ImpactRef can either be a reference to a pre-defined
 * level in the category definition (i.e. a discrete integer that corresponds to a predefined
 * CategoryLevel's ordinal value) or an arbitrary number that lies within the category definition's
 * boundaries (i.e. a decimal).
 *
 * <p>This is because impacts can be defined as either discrete predefined levels or as a continuous
 * value such as a monetary loss.
 *
 * <p>As of now, only discrete reference values are supported.
 */
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@Valid
@EqualsAndHashCode
public class ImpactRef {

  @Getter @PositiveOrZero BigDecimal idRef;

  public static ImpactRef from(CategoryLevel cl) {
    return new ImpactRef(new BigDecimal(cl.getOrdinalValue()));
  }
}
