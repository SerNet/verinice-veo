/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Jochen Kemnade.
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
package org.veo.adapter.presenter.api.dto;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.veo.core.Translations;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Value;

@Value
@Schema(description = "Translations for an entity type")
public class TranslationsDto implements Translations {

  private final Map<Locale, TranslationDto> translationsByLanguage = new HashMap<>();

  public void add(Locale lang, Map<String, String> translations) {
    TranslationDto translationsForLanguage =
        translationsByLanguage.computeIfAbsent(lang, k -> new TranslationDto(new HashMap<>()));
    translationsForLanguage.getTranslations().putAll(translations);
  }

  @JsonProperty("lang")
  @Schema(description = "The keys are language codes, the values are the translations")
  public Map<Locale, TranslationDto> getTranslationsByLanguage() {
    return translationsByLanguage;
  }

  @Override
  public void add(Locale language, String key, String message) {
    add(language, Map.of(key, message));
  }

  @Override
  public Optional<String> get(Locale language, String key) {
    return Optional.ofNullable(translationsByLanguage.get(language))
        .map(v -> v.getTranslations().get(key));
  }
}
