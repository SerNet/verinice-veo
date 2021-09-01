/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Urs Zeidler.
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
package org.veo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author urszeidler
 */
public class SchemaMerger {
    private ObjectMapper mapper = new ObjectMapper();
    private Map<String, Map<String, JsonNode>> customAspects;
    private Map<String, Map<String, JsonNode>> customlinks;

    public SchemaMerger(Path basePathCustom) throws IOException {
        Path customEntitesPath = basePathCustom.resolve("custom/aspects/");
        Path linkPath = basePathCustom.resolve("custom/link/");
        customAspects = readSchemas(customEntitesPath);
        customlinks = readSchemas(linkPath);
    }

    public void extendSchema(JsonNode schema, String type) {
        var aspectExtensions = Optional.ofNullable(customAspects.get(type))
                                       .orElseGet(Collections::emptyMap);
        processCustomAspects(aspectExtensions, schema);

        var linkExtensions = Optional.ofNullable(customlinks.get(type))
                                     .orElseGet(Collections::emptyMap);
        processLinkSchemas(linkExtensions, schema);
    }

    /**
     * Add all the given custom aspect schemas in the customAspects section of the
     * given entity schema.
     */
    private void processCustomAspects(Map<String, JsonNode> customAspectSchemas,
            JsonNode entitySchema) {
        List<JsonNode> aspectNodes = entitySchema.findValues("customAspects");
        assert (aspectNodes.size() == 1);
        ObjectNode propertiesNode = Objects.requireNonNull(addPropertiesNode((ObjectNode) aspectNodes.get(0)),
                                                           "Unable to find target node in "
                                                                   + aspectNodes);
        customAspectSchemas.forEach(propertiesNode::set);
    }

    /**
     * Add all the given link schemas in the link section of the given entity
     * schema.
     */
    private void processLinkSchemas(Map<String, JsonNode> linkSchemas, JsonNode entitySchema) {
        List<JsonNode> linkNodes = entitySchema.findValues("links");
        assert (linkNodes.size() == 1);
        ObjectNode jsonNode = addPropertiesNode((ObjectNode) linkNodes.get(0));
        linkSchemas.forEach(jsonNode::set);
    }

    /**
     * Returns the node where to add the new schemas.
     */
    private ObjectNode addPropertiesNode(ObjectNode aspectNode) {
        ObjectNode result = aspectNode.objectNode();
        aspectNode.set("properties", result);
        return result;
    }

    /**
     * Returns a map of entity type name to map of schema name to schema. The entity
     * type name is made by the sub directory of the customEntitiesPath and the
     * schema name is made by the schema file name.
     */
    private Map<String, Map<String, JsonNode>> readSchemas(Path customEntitiesPath)
            throws IOException {
        try (Stream<Path> dirs = Files.list(customEntitiesPath)
                                      .filter(Files::isDirectory)) {
            return dirs.collect(Collectors.toMap((Path dir) -> dir.getFileName()
                                                                  .toString(),
                                                 dir -> {
                                                     try (Stream<Path> files = Files.list(dir)) {
                                                         return files.collect(Collectors.toMap(file -> file.getFileName()
                                                                                                           .toString()
                                                                                                           .replace(".json",
                                                                                                                    ""),
                                                                                               file -> {
                                                                                                   try {
                                                                                                       return mapper.readTree(file.toFile());
                                                                                                   } catch (IOException e) {
                                                                                                       throw new RuntimeException(
                                                                                                               e);
                                                                                                   }
                                                                                               }));
                                                     } catch (IOException e) {
                                                         throw new RuntimeException(e);
                                                     }
                                                 }));
        }
    }
}