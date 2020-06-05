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

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author urszeidler
 *
 */
public class SchemaMerger {
    private Map<String, JsonNode> schemaMap = new HashMap<>();
    private ObjectMapper mapper = new ObjectMapper();

    public void processPath(String basePathCustom, Path basePathEntities, Path targetPath)
            throws IOException {
        File customEntitesPath = new File(basePathCustom + "/custom/entity/");
        File linkPath = new File(basePathCustom + "/custom/link/");

        Map<String, List<JsonNode>> customEn = readSchemas(customEntitesPath);

        for (Entry<String, List<JsonNode>> entry : customEn.entrySet()) {
            List<JsonNode> value = entry.getValue();
            String entityName = entry.getKey();
            JsonNode entitySchema = getEntitySchema(entityName, basePathEntities);
            processCustomAspects(value, entitySchema);
        }

        Map<String, List<JsonNode>> customlinks = readSchemas(linkPath);
        for (Entry<String, List<JsonNode>> entry : customlinks.entrySet()) {
            String entityName = entry.getKey();
            List<JsonNode> schemas = entry.getValue();
            JsonNode entitySchema = getEntitySchema(entityName, basePathEntities);
            processLinkSchemas(schemas, entitySchema);
        }

        // write the schemas to files
        for (Entry<String, JsonNode> jsonNode : schemaMap.entrySet()) {
            ObjectWriter writer = mapper.writer(new DefaultPrettyPrinter());
            Path target = targetPath.resolve(jsonNode.getKey() + ".json");
            writer.writeValue(target.toFile(), jsonNode.getValue());
        }
    }

    /**
     * Add all the given custom aspect schemas in the link section of the given
     * entity schema.
     */
    private void processCustomAspects(List<JsonNode> customAspectSchemas, JsonNode entitySchema) {
        List<JsonNode> aspectNodes = entitySchema.findValues("customAspects");
        assert (aspectNodes.size() == 1);
        ObjectNode jsonNode = (ObjectNode) extractTargetNode(aspectNodes);

        for (JsonNode customSchema : customAspectSchemas) {
            String type = extractEntityType(customSchema);
            jsonNode.set(type, customSchema);
        }
    }

    /**
     * Add all the given link schemas in the link section of the given entity
     * schema.
     *
     * @param linkSchemas
     * @param entitySchema
     */
    private void processLinkSchemas(List<JsonNode> linkSchemas, JsonNode entitySchema) {
        List<JsonNode> aspectNodes = entitySchema.findValues("links");
        assert (aspectNodes.size() == 1);
        ObjectNode jsonNode = (ObjectNode) extractTargetNode(aspectNodes);

        for (JsonNode customSchema : linkSchemas) {
            String type = extractLinkType(customSchema);
            customSchema = addArrayDeclarationToLink(customSchema);
            jsonNode.set(type, customSchema);
        }
    }

    /**
     * Add the necessary array declaration around the basic schema
     */
    private JsonNode addArrayDeclarationToLink(JsonNode customSchema) {
        // TODO: implement
        // {
        // "type": "array",
        // "items": {
        // "type": "object",
        return customSchema;
        // }
        // }
    }

    /**
     * Returns the node where to add the new schemas.
     */
    private JsonNode extractTargetNode(List<JsonNode> aspectNodes) {
        return aspectNodes.get(0)
                          .get("properties");
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
    private String extractEntityType(JsonNode customSchema) {
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
    private Map<String, List<JsonNode>> readSchemas(File customEntitesPath) throws IOException {
        Map<String, List<JsonNode>> s = new HashMap<>();
        if (customEntitesPath == null) {
            return s;
        }

        File[] dirs = customEntitesPath.listFiles();
        if (dirs == null) {
            return s;
        }
        for (File dir : dirs) {
            File[] files = dir.listFiles();
            if (files == null) {
                continue;
            }
            String targetName = dir.getName();
            ArrayList<JsonNode> schemaForTypes = new ArrayList<>(files.length);

            for (File schemaFile : files) {
                JsonNode schemaMap = mapper.readTree(schemaFile);
                schemaForTypes.add(schemaMap);
            }
            s.put(targetName, schemaForTypes);
        }
        return s;
    }

    /**
     * Get the raw Schema for an entity.
     */
    private JsonNode getEntitySchema(String entityName, Path baseSchemaPath) {
        return schemaMap.computeIfAbsent(entityName, k -> {

            Path schemaPath = baseSchemaPath.resolve(k + ".json");
            try {
                return mapper.readTree(schemaPath.toFile());
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

    }

}
