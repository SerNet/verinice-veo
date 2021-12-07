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
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.veo.adapter.presenter.api.dto.TranslationsDto;
import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.core.entity.EntityType;
import org.veo.core.entity.definitions.ElementTypeDefinition;
import org.veo.core.service.EntitySchemaService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Returns a static JSON file of the full entity from classpath. Ignores the
 * 'domain' parameter. Delivers the full JSON. Ignores the language parameter,
 * returning the full translation file.
 */
@Slf4j
@RequiredArgsConstructor
public class EntitySchemaServiceImpl implements EntitySchemaService {

    private static final List<String> VALID_TYPE_SINGULAR_TERMS = EntityType.ELEMENT_TYPES.stream()
                                                                                          .map(EntityType::getSingularTerm)
                                                                                          .collect(Collectors.toList());
    private final EntitySchemaGenerator generateEntitytSchema;

    @Override
    public String findSchema(String type, Set<Domain> domains) {
        if (!VALID_TYPE_SINGULAR_TERMS.contains(type)) {
            throw new IllegalArgumentException(
                    String.format("Type \"%s\" is not a valid schema.", type));
        }
        log.debug("Getting dynamic JSON schema for type: {}", type);
        return generateEntitytSchema.createSchema(type, domains);
    }

    @Override
    public String roleFilter(List<String> roles, String inputSchema) {
        return inputSchema; // TODO VEO-966 filter schema elements by user role
    }

    @Override
    public TranslationsDto findTranslations(Client client, Set<String> languages) {
        log.debug("Getting full static translation content, ignoring requested language filter: {}",
                  languages);
        TranslationsDto translations = new TranslationsDto();
        for (Domain domain : client.getDomains()) {
            for (ElementTypeDefinition definition : domain.getElementTypeDefinitions()) {
                Map<String, Map<String, String>> translationsForDefinition = definition.getTranslations();
                for (Entry<String, Map<String, String>> entry : translationsForDefinition.entrySet()) {
                    String lang = entry.getKey();
                    // TODO VEO-526 evaluate languages parameter
                    translations.add(lang, entry.getValue());
                }
            }
        }
        return translations;
    }
}
