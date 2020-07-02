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
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.service.EntitySchemaService;
import org.veo.core.service.SchemaIdentifiersDTO;

/**
 * Returns a static JSON file of the full entity from classpath. Ignores the
 * 'domain' parameter. Delivers the full JSON. Ignores the language parameter,
 * returning the full translation file.
 */
@Slf4j
public class EntitySchemaServiceClassPathImpl implements EntitySchemaService {

    private static final String SCHEMA_FILES_PATH = "/schemas/entity/";
    private static final List<String> VALID_SCHEMAS = List.of("process", "asset", "person",
                                                              "control");

    @Override
    public String findSchema(String type, List<String> domains) {
        if (VALID_SCHEMAS.stream()
                         .filter(s -> s.equals(type))
                         .count() != 1) {
            throw new IllegalArgumentException("Type is not a valid schema.");
        }
        log.debug("Getting static JSON schema file for type: " + type);
        return extract(SCHEMA_FILES_PATH + type.substring(0, 1)
                                               .toUpperCase()
                + type.substring(1) + ".json");
    }

    @Override
    public String roleFilter(List<String> roles, String inputSchema) {
        return inputSchema; // TODO filter schema elements by user role
    }

    @Override
    public String findTranslations(Set<String> languages) {
        log.debug("Getting full static translation file, ignoring requested language filter: "
                + languages);
        return extract("/lang/lang.json");
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
        return new SchemaIdentifiersDTO(VALID_SCHEMAS);
    }
}
