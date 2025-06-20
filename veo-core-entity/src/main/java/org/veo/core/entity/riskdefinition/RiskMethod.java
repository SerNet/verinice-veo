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

import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.veo.core.entity.Constraints;
import org.veo.core.entity.Translated;
import org.veo.core.entity.TranslationProvider;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/** Defines the method to determine a risk value from the {@link RiskDefinition}. */
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
// TODO #3935: when all domains are migrated to the new structure this ignoreProperties can be
// removed
@JsonIgnoreProperties({"description", "impactMethod"})
public class RiskMethod implements TranslationProvider<RiskMethod.ImpactMethodAndDescription> {
  @ToString.Include @EqualsAndHashCode.Include
  private Translated<ImpactMethodAndDescription> translations = new Translated<>();

  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  static class ImpactMethodAndDescription {
    @Size(max = Constraints.DEFAULT_DESCRIPTION_MAX_LENGTH)
    private String description;

    @Size(max = Constraints.DEFAULT_STRING_MAX_LENGTH)
    private String impactMethod;
  }
}
