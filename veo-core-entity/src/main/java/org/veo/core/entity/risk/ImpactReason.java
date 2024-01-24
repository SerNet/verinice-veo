/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2024  Alexander Koderman
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

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Valid
public enum ImpactReason {

  /** The impact is higher because it accumulates from multiple sources. */
  CUMULATIVE(Constants.CUMULATIVE),

  /** The impact is lower because it is distributed over multiple systems. */
  DISTRIBUTIVE(Constants.DISTRIBUTIVE),

  /**
   * The user specifies the impact for more complex reasons given in the {@code
   * ImpactValues#explanation}
   */
  MANUAL(Constants.MANUAL);

  private final String translationKey;

  public static ImpactReason fromTranslationKey(String name) {
    if (name != null) {
      for (ImpactReason r : values()) {
        if (name.equalsIgnoreCase(r.getTranslationKey())) {
          return r;
        }
      }
    }
    throw new IllegalArgumentException("No impact reason with name %s found".formatted(name));
  }

  public static class Constants {
    public static final String CUMULATIVE = "impact_reason_cumulative";
    public static final String DISTRIBUTIVE = "impact_reason_distributive";
    public static final String MANUAL = "impact_reason_manual";
  }
}
