/*******************************************************************************
 * Copyright (c) 2020 Urs Zeidler.
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
package org.veo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
    private Map<String, List<JsonNode>> customAspects;
    private Map<String, List<JsonNode>> customlinks;

    public SchemaMerger(Path basePathCustom) throws IOException {
        Path customEntitesPath = basePathCustom.resolve("custom/aspects/");
        Path linkPath = basePathCustom.resolve("custom/link/");
        customAspects = readSchemas(customEntitesPath);
        customlinks = readSchemas(linkPath);
    }

    public void extendSchema(JsonNode schema, String type) {
        Optional.ofNullable(customAspects.get(type))
                .ifPresent(extensions -> processCustomAspects(extensions, schema));
        Optional.ofNullable(customlinks.get(type))
                .ifPresent(extensions -> processLinkSchemas(extensions, schema));
    }

    /**
     * Add all the given custom aspect schemas in the customAspects section of the
     * given entity schema.
     */
    private void processCustomAspects(List<JsonNode> customAspectSchemas, JsonNode entitySchema) {
        List<JsonNode> aspectNodes = entitySchema.findValues("customAspects");
        assert (aspectNodes.size() == 1);
        ObjectNode propertiesNode = Objects.requireNonNull(addPropertiesNode((ObjectNode) aspectNodes.get(0)),
                                                           "Unable to find target node in "
                                                                   + aspectNodes);
        for (JsonNode customSchema : customAspectSchemas) {
            String type = extractAspectType(customSchema);
            propertiesNode.set(type, customSchema);
        }
    }

    /**
     * Add all the given link schemas in the link section of the given entity
     * schema.
     */
    private void processLinkSchemas(List<JsonNode> linkSchemas, JsonNode entitySchema) {
        List<JsonNode> linkNodes = entitySchema.findValues("links");
        assert (linkNodes.size() == 1);
        ObjectNode jsonNode = addPropertiesNode((ObjectNode) linkNodes.get(0));

        for (JsonNode customSchema : linkSchemas) {
            String type = extractLinkType(customSchema);
            jsonNode.set(type, customSchema);
        }
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
     * Extract the type field from a customLink schema.
     */
    private String extractLinkType(JsonNode customSchema) {
        return customSchema.get("items")
                           .get("properties")
                           .get("type")
                           .get("enum")
                           .get(0)
                           .asText();
    }

    /**
     * Extract the type field from a customProperties schema.
     */
    private String extractAspectType(JsonNode customSchema) {
        return customSchema.get("properties")
                           .get("type")
                           .get("enum")
                           .get(0)
                           .asText();
    }

    /**
     * Returns the map of entityTypeName<->List<CustomSchemas>. The entityTypeName
     * is made by the sub directory of the customEntitesPath.
     */
    private Map<String, List<JsonNode>> readSchemas(Path customEntitesPath) throws IOException {
        try (Stream<Path> dirs = Files.list(customEntitesPath)
                                      .filter(Files::isDirectory)) {
            return dirs.collect(Collectors.toMap((Path dir) -> dir.getFileName()
                                                                  .toString(),
                                                 dir -> {
                                                     try (Stream<Path> files = Files.list(dir)) {
                                                         return files.map(schemaFile -> {
                                                             try {
                                                                 return mapper.readTree(schemaFile.toFile());
                                                             } catch (IOException e) {
                                                                 throw new RuntimeException(e);
                                                             }
                                                         })
                                                                     .collect(Collectors.toList());
                                                     } catch (IOException e) {
                                                         throw new RuntimeException(e);
                                                     }
                                                 }));
        }
    }
}