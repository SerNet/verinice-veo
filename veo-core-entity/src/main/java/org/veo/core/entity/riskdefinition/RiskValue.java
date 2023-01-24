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
package org.veo.core.entity.riskdefinition;

import static org.veo.core.entity.Constraints.DEFAULT_CONSTANT_MAX_LENGTH;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.veo.core.entity.Nameable;
import org.veo.core.entity.TranslationMap;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * A RiskValue is a specific type of {@link DiscreteValue}. It has a symbolic risk which needs to be
 * unique and is used to define the values of the {@link RiskDefinition#getRiskValues()} and the
 * values of the {@link CategoryDefinition#getValueMatrix()}
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(onlyExplicitlyIncluded = true)
public class RiskValue extends DiscreteValue {

  public RiskValue(
      int ordinalValue,
      String htmlColor,
      @NotNull(message = "A symbolic risk value must be present.")
          @Size(max = DEFAULT_CONSTANT_MAX_LENGTH)
          String symbolicRisk) {
    super(htmlColor);
    this.symbolicRisk = symbolicRisk;
    setOrdinalValue(ordinalValue);
  }

  public RiskValue(
      int ordinalValue,
      String htmlColor,
      @NotNull(message = "A symbolic risk value must be present.")
          @Size(max = DEFAULT_CONSTANT_MAX_LENGTH)
          String symbolicRisk,
      TranslationMap translations) {
    super(ordinalValue, htmlColor, translations);
    this.symbolicRisk = symbolicRisk;
  }

  @NotNull(message = "A symbolic risk value must be present.")
  @Size(max = Nameable.NAME_MAX_LENGTH)
  @ToString.Include
  private String symbolicRisk;
}
