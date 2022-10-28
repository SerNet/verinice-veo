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

import static org.veo.core.entity.riskdefinition.DeprecatedAttributes.ABBREVIATION;
import static org.veo.core.entity.riskdefinition.DeprecatedAttributes.DESCRIPTION;
import static org.veo.core.entity.riskdefinition.DeprecatedAttributes.NAME;

import java.util.HashMap;
import java.util.Map;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

import org.veo.core.entity.Constraints;
import org.veo.core.entity.TranslationProvider;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * A discrete value represents a value in the risk matrix or a level of a DimensionDefinition. It
 * defines the basic values like name and html-color. The ordinal value is managed by the contained
 * object and the same as the position in the corresponding list. The identity aka equals and
 * hashcode are defined by id and name.
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class DiscreteValue implements TranslationProvider {

  public DiscreteValue(@Size(max = 255) String htmlColor) {
    super();
    this.htmlColor = htmlColor;
  }

  @EqualsAndHashCode.Include @ToString.Include private int ordinalValue;

  @Size(max = Constraints.DEFAULT_STRING_MAX_LENGTH)
  @ToString.Include
  private String htmlColor;

  @ToString.Exclude @NotNull @NotEmpty
  private Map<String, Map<String, String>> translations = new HashMap<>();
  /**
   * Provide compatibility with old clients and data structure. This will read the old data and
   * transform it to the new data.
   */
  @JsonAnySetter
  @Deprecated
  // TODO: VEO-1739 remove
  public void setOldValues(String name, String value) {
    if (value == null) {
      return;
    }
    if (DeprecatedAttributes.DEPRECATED_ATTRIBUTES.contains(name)) {
      getDefaultTranslation().put(name, value);
    } else {
      throw new IllegalArgumentException("No property " + name);
    }
  }

  @Deprecated
  private Map<String, String> getDefaultTranslation() {
    return translations.computeIfAbsent("de", t -> new HashMap<String, String>());
  }

  @Deprecated
  @JsonProperty(access = Access.READ_ONLY)
  public String getName() {
    return getDefaultTranslation().get(NAME);
  }

  @Deprecated
  @JsonProperty(access = Access.READ_ONLY)
  public String getAbbreviation() {
    return getDefaultTranslation().get(ABBREVIATION);
  }

  @Deprecated
  @JsonProperty(access = Access.READ_ONLY)
  public String getDescription() {
    return getDefaultTranslation().get(DESCRIPTION);
  }
}
