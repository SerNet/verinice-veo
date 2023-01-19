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
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

import org.veo.core.entity.Constraints;
import org.veo.core.entity.TranslationMap;
import org.veo.core.entity.TranslationProvider;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * The basic class for a dimension definition. A dimension definition has an unique id and can work
 * with {@link DiscreteValue} as level.
 */
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@Data
public class DimensionDefinition implements TranslationProvider {
  protected static final String DIMENSION_PROBABILITY = "Prob";
  protected static final String DIMENSION_IMPLEMENTATION_STATE = "Ctr";

  @NotNull(message = "An id must be present.")
  @Size(max = Constraints.DEFAULT_CONSTANT_MAX_LENGTH)
  @EqualsAndHashCode.Include
  @ToString.Include
  private String id;

  @NotNull @NotEmpty private TranslationMap translations = new TranslationMap();

  public DimensionDefinition(String id) {
    this.id = id;
  }

  public DimensionDefinition(String id, Map<String, Map<String, String>> translations) {
    this.id = id;
    this.translations = TranslationMap.of(translations);
  }

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

  public Map<Locale, Map<String, String>> getTranslations() {
    return translations.getTranslations();
  }

  @Deprecated
  private Map<String, String> getDefaultTranslation() {
    return translations
        .getTranslations()
        .computeIfAbsent(new Locale.Builder().setLanguage("de").build(), t -> new HashMap<>());
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

  /** Initialize the ordinal value of each DiscreteValue in the list. */
  static void initLevel(List<? extends DiscreteValue> discretValues) {
    for (int i = 0; i < discretValues.size(); i++) {
      discretValues.get(i).setOrdinalValue(i);
    }
  }
}
