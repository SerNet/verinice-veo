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
package org.veo.core.entity;

import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public interface TranslationProvider<T> {

  static <T> Map<Locale, T> convertLocales(Map<String, T> description) {
    return description.entrySet().stream()
        .collect(Collectors.toMap(e -> Locale.forLanguageTag(e.getKey()), Map.Entry::getValue));
  }

  Translated<T> getTranslations();

  default T getTranslations(Locale locale) {
    return getTranslations().getTranslations().get(locale);
  }
}
