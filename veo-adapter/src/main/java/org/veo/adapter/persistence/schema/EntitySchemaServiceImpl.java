/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Alexander Koderman.
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
package org.veo.adapter.persistence.schema;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.veo.adapter.presenter.api.dto.TranslationsDto;
import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.core.entity.EntityType;
import org.veo.core.service.EntitySchemaService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Returns a static JSON file of the full entity from classpath. Ignores the 'domain' parameter.
 * Delivers the full JSON. Ignores the language parameter, returning the full translation file.
 */
@Slf4j
@RequiredArgsConstructor
public class EntitySchemaServiceImpl implements EntitySchemaService {

  private static final List<String> VALID_TYPE_SINGULAR_TERMS =
      EntityType.ELEMENT_TYPES.stream().map(EntityType::getSingularTerm).toList();
  private final EntitySchemaGenerator generateEntitytSchema;

  @Override
  public String findSchema(String type, Set<Domain> domains) {
    if (!VALID_TYPE_SINGULAR_TERMS.contains(type)) {
      throw new IllegalArgumentException(String.format("Type \"%s\" is not a valid schema.", type));
    }
    log.debug("Getting dynamic JSON schema for type: {}", type);
    return generateEntitytSchema.createSchema(type, domains);
  }

  @Override
  public String roleFilter(List<String> roles, String inputSchema) {
    return inputSchema; // TODO VEO-966 filter schema elements by user role
  }

  @Override
  public TranslationsDto findTranslations(Client client, Set<Locale> requestedLanguages) {
    log.debug("Getting translation content for requested languages: {}", requestedLanguages);
    TranslationsDto translations = new TranslationsDto();
    client.getDomains().stream()
        .flatMap(domain -> domain.getElementTypeDefinitions().stream())
        .flatMap(def -> def.getTranslations().entrySet().stream())
        .filter(langEntry -> isRequested(requestedLanguages, langEntry.getKey()))
        .forEach(langEntry -> translations.add(langEntry.getKey(), langEntry.getValue()));
    return translations;
  }

  static boolean isRequested(Set<Locale> requestedLanguages, Locale languageEntry) {
    if (requestedLanguages.contains(languageEntry)) return true;

    // if no match for language+region, try to find generic language entry:
    return requestedLanguages.stream()
        .map(Locale::getLanguage)
        .anyMatch(
            l -> l.equals(languageEntry.getLanguage()) && languageEntry.getCountry().isEmpty());
  }
}
