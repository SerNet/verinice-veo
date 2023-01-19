/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Alexander Koderman
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
package org.veo.core.entity;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A collection of translation-keys and -values for different languages. Defines the representation
 * of such a collection in JSON form.
 */
@Data
@NoArgsConstructor
public class TranslationMap {

  @NotNull @JsonValue private Map<Locale, Map<String, String>> translations = new HashMap<>();

  @JsonCreator
  public TranslationMap(Map<Locale, Map<String, String>> themap) {
    this.translations = themap;
  }

  public static TranslationMap of(Map<String, Map<String, String>> translations) {
    return new TranslationMap(TranslationProvider.convertLocales(translations));
  }
}
