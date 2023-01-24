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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

import org.veo.core.entity.TranslationMap;
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
public class RiskMethod implements TranslationProvider {
  @ToString.Include @EqualsAndHashCode.Include
  private TranslationMap translations = new TranslationMap();

  /**
   * Provide compatibility with old clients and data structure. This will read the old data and
   * transform it to the new data.
   */
  @Deprecated
  @JsonAnySetter
  // TODO: VEO-1739 remove
  public void setOldValues(String name, String value) {
    if (value == null) {
      return;
    }
    if ("impactMethod".equals(name)) {
      getDefaultTranslation().put(name, value);
    } else if ("description".equals(name)) {
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

  /**
   * Provide compatibility with old clients and data structure. This will provide the old data
   * transformed by the new data.
   */
  @Deprecated
  @JsonProperty(access = Access.READ_ONLY)
  public String getDescription() {
    return getDefaultTranslation().get("description");
  }

  /**
   * Provide compatibility with old clients and data structure. This will provide the old data
   * transformed by the new data.
   */
  @Deprecated
  @JsonProperty(access = Access.READ_ONLY)
  public String getImpactMethod() {
    return getDefaultTranslation().get("impactMethod");
  }
}
