/*******************************************************************************
 * Copyright (c) 2020 Alexander Koderman.
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.veo.adapter.persistence.schema;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.veo.core.entity.ModelObjectType;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.service.EntitySchemaService;
import org.veo.core.service.SchemaIdentifiersDTO;

import io.swagger.v3.core.util.Json;
import lombok.extern.slf4j.Slf4j;

/**
 * Returns a static JSON file of the full entity from classpath. Ignores the
 * 'domain' parameter. Delivers the full JSON. Ignores the language parameter,
 * returning the full translation file.
 */
@Slf4j
public class EntitySchemaServiceClassPathImpl implements EntitySchemaService {

    private static final String SCHEMA_FILES_PATH = "/schemas/entity/";
    private static final Set<String> VALID_CLASS_NAMES = ModelObjectType.ENTITY_TYPES.stream()
                                                                                     .map(Class::getSimpleName)
                                                                                     .collect(Collectors.toSet());

    private final Map<String, Map<String, Map<String, String>>> constantTranslations;

    @SuppressWarnings("unchecked")
    public EntitySchemaServiceClassPathImpl() throws IOException {
        try (InputStream is = EntitySchemaServiceClassPathImpl.class.getResourceAsStream("/lang/lang.json")) {
            constantTranslations = Json.mapper()
                                       .readValue(is, Map.class);
        }
        log.info("constantTranslations = {}", constantTranslations);
    }

    @Override
    public String findSchema(String type, List<String> domains) {
        var typeClassName = mapFirst(type, Character::toUpperCase);
        if (!VALID_CLASS_NAMES.contains(typeClassName)) {
            throw new IllegalArgumentException(
                    String.format("Type \"%s\" is not a valid schema.", type));
        }
        log.debug("Getting static JSON schema file for type: " + type);
        return extract(SCHEMA_FILES_PATH + typeClassName + ".json");
    }

    @Override
    public String roleFilter(List<String> roles, String inputSchema) {
        return inputSchema; // TODO filter schema elements by user role
    }

    @Override
    public String findTranslations(Set<String> languages) {

        Map<String, Map<String, String>> lang = new HashMap<>();

        for (String language : languages) {
            lang.put(language, constantTranslations.get("lang")
                                                   .get(language));
        }

        try {
            for (String typeClassName : VALID_CLASS_NAMES) {
                String schemaFile = SCHEMA_FILES_PATH + typeClassName + ".json";
                log.info("Reading schema {}", schemaFile);
                try (InputStream is = EntitySchemaServiceClassPathImpl.class.getResourceAsStream(schemaFile)) {
                    JsonNode schema = Json.mapper()
                                          .readTree(is);
                    ObjectNode customAspects = (ObjectNode) schema.get("properties")
                                                                  .get("customAspects")
                                                                  .get("properties");
                    handleProperties(customAspects, false, lang, languages);
                    ObjectNode links = (ObjectNode) schema.get("properties")
                                                          .get("links")
                                                          .get("properties");
                    if (links != null) {
                        handleProperties(links, true, lang, languages);
                    }
                }
            }

            return Json.mapper()
                       .writeValueAsString(Map.of("lang", lang));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load translations for " + languages, e);
        }

    }

    protected void handleProperties(ObjectNode propertiesNode, boolean isCustomLink,
            Map<String, Map<String, String>> langObject, Set<String> languages) {
        Iterator<Entry<String, JsonNode>> fieldIteator = propertiesNode.fields();
        while (fieldIteator.hasNext()) {
            Entry<String, JsonNode> field = fieldIteator.next();
            String id = field.getKey();
            log.info("Reading custom aspect {}", id);
            JsonNode translations = isCustomLink ? field.getValue()
                                                        .get("items")
                                                        .get("translations")
                    : field.getValue()
                           .get("translations");
            for (String language : languages) {
                Map<String, String> langEntry = langObject.get(language);
                Optional.ofNullable(translations.get(language))
                        .ifPresent(translationsForLanguage -> {
                            Iterator<Entry<String, JsonNode>> translationIt = translationsForLanguage.fields();
                            while (translationIt.hasNext()) {
                                Entry<String, JsonNode> translation = translationIt.next();
                                langEntry.put(translation.getKey(), translation.getValue()
                                                                               .asText());
                            }

                        });
            }
        }
    }

    private String extract(final String file) {
        log.debug("Loading file form classpath: " + file);
        try (BufferedReader br = new BufferedReader(new InputStreamReader(this.getClass()
                                                                              .getResourceAsStream(file),
                StandardCharsets.UTF_8

        ))) {
            return br.lines()
                     .collect(Collectors.joining("\n"));
        } catch (IOException e) {
            throw new NotFoundException("Stored JSON file has wrong encoding.");
        }
    }

    @Override
    public SchemaIdentifiersDTO listValidSchemaNames() {
        return new SchemaIdentifiersDTO(VALID_CLASS_NAMES.stream()
                                                         .map(s -> mapFirst(s,
                                                                            Character::toLowerCase))
                                                         .collect(Collectors.toList()));
    }

    private String mapFirst(String str, Function<Character, Character> f) {
        return f.apply(str.charAt(0)) + str.substring(1);
    }
}
