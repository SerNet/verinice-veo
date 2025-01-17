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

import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import org.veo.core.entity.riskdefinition.RiskValue;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Depending on the category definition, a RiskRef can either be a reference to a pre-defined level
 * in the category definition (i.e. a discrete integer that corresponds to a predefined
 * CategoryLevel's ordinal value) or an arbitrary number that lies within the category definition's
 * boundaries (i.e. a decimal).
 *
 * <p>This is because risks can be defined as either discrete predefined levels or as a continuous
 * value such as a monetary loss.
 *
 * <p>As of now, only discrete reference values are supported.
 */
@Valid
@EqualsAndHashCode
@ToString
public class RiskRef {

  @JsonCreator
  RiskRef(BigDecimal idRef) {
    this.idRef = idRef;
  }

  @PositiveOrZero @Getter @JsonValue BigDecimal idRef;

  public static RiskRef from(RiskValue rd) {
    return new RiskRef(new BigDecimal(rd.getOrdinalValue()));
  }
}
