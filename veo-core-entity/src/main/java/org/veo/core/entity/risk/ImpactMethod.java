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

/**
 * The method used to automatically calculate potential impact values based on linked elements or
 * context. The only implemented method currently is "high water mark". This class serves to
 * reference the method and to translate the method name to a human readable string and may be
 * replaced by a dynamic implementation where the method can be parameterized in the risk definition
 * in the future.
 */
@Getter
@AllArgsConstructor
@Valid
public enum ImpactMethod {

  /** The impact is calculated by taking the highest impact value of all linked elements. */
  HIGH_WATER_MARK(Constants.HIGH_WATER_MARK);

  private final String translationKey;

  public static ImpactMethod fromTranslationKey(String name) {
    if (name != null) {
      for (ImpactMethod r : values()) {
        if (name.equalsIgnoreCase(r.getTranslationKey())) {
          return r;
        }
      }
    }
    throw new IllegalArgumentException("No impact reason with name %s found".formatted(name));
  }

  public static class Constants {
    public static final String HIGH_WATER_MARK = "impact_method_high_water_mark";
  }
}
