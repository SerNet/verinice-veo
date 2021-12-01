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
package org.veo.adapter.service;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.veo.core.entity.EntityType;
import org.veo.core.entity.definitions.CustomAspectDefinition;
import org.veo.core.entity.definitions.ElementTypeDefinition;
import org.veo.core.entity.definitions.LinkDefinition;
import org.veo.core.entity.definitions.SubTypeDefinition;
import org.veo.core.entity.transform.EntityFactory;

/**
 * This class serves as a bridge between {@link ElementTypeDefinition} and the
 * object JSON schema used by the Object Schema Editor. It can update an
 * existing element type definition with a Jackson node representing the schema.
 */
public class ObjectSchemaParser {

    private static final ObjectMapper OBJECTMAPPER = new ObjectMapper();

    private static final String PROPERTIES = "properties";

    private final EntityFactory entityFactory;

    public ObjectSchemaParser(EntityFactory entityFactory) {
        this.entityFactory = entityFactory;
    }

    public ElementTypeDefinition parseTypeDefinitionFromObjectSchema(EntityType type,
            JsonNode schemaNode) throws JsonProcessingException {
        JsonNode properties = schemaNode.get(PROPERTIES);
        ElementTypeDefinition typeDefinition = entityFactory.createElementTypeDefinition(type.getSingularTerm(),
                                                                                         null);
        typeDefinition.setSubTypes(extractSubTypeDefinitions(properties));
        typeDefinition.setCustomAspects(extractCustomAspectDefinitions(properties));
        typeDefinition.setLinks(extractLinkDefinitions(properties));
        return typeDefinition;
    }

    private Map<String, SubTypeDefinition> extractSubTypeDefinitions(JsonNode properties) {
        JsonNode domains = properties.get("domains");

        JsonNode allOf = domains.get("patternProperties")
                                .get("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")
                                .get("allOf");

        return StreamSupport.stream(allOf.spliterator(), false)
                            .collect(Collectors.toMap(subTypeInfo -> subTypeInfo.get("if")
                                                                                .get(PROPERTIES)
                                                                                .get("subType")
                                                                                .get("const")
                                                                                .asText(),
                                                      subTypeInfo -> {
                                                          JsonNode statusValues = subTypeInfo.get("then")
                                                                                             .get(PROPERTIES)
                                                                                             .get("status")
                                                                                             .get("enum");
                                                          SubTypeDefinition subTypeDefinition = new SubTypeDefinition();
                                                          subTypeDefinition.setStatuses(StreamSupport.stream(statusValues.spliterator(),
                                                                                                             false)
                                                                                                     .map(JsonNode::asText)
                                                                                                     .collect(Collectors.toSet()));
                                                          return subTypeDefinition;
                                                      }));
    }

    private Map<String, CustomAspectDefinition> extractCustomAspectDefinitions(JsonNode properties)
            throws JsonProcessingException {
        JsonNode customAspectsProperties = properties.get("customAspects")
                                                     .get(PROPERTIES);
        Iterator<Entry<String, JsonNode>> aspectIt = customAspectsProperties.fields();
        Map<String, CustomAspectDefinition> customAspects = new HashMap<>(
                customAspectsProperties.size());
        while (aspectIt.hasNext()) {
            Entry<String, JsonNode> entry = aspectIt.next();
            String aspectName = entry.getKey();
            JsonNode aspectAttributes = entry.getValue()
                                             .get(PROPERTIES)
                                             .get("attributes")
                                             .get(PROPERTIES);
            CustomAspectDefinition aspectDefinition = new CustomAspectDefinition();
            Iterator<Entry<String, JsonNode>> attributeIt = aspectAttributes.fields();
            Map<String, Object> attributeSchemas = new HashMap<>(aspectAttributes.size());
            while (attributeIt.hasNext()) {
                Entry<String, JsonNode> attributeEntry = attributeIt.next();
                attributeSchemas.put(attributeEntry.getKey(),
                                     OBJECTMAPPER.treeToValue(attributeEntry.getValue(),
                                                              Map.class));
            }
            aspectDefinition.setAttributeSchemas(attributeSchemas);
            customAspects.put(aspectName, aspectDefinition);
        }
        return customAspects;
    }

    private Map<String, LinkDefinition> extractLinkDefinitions(JsonNode properties)
            throws JsonProcessingException {
        JsonNode linksProperties = properties.get("links")
                                             .get(PROPERTIES);
        Iterator<Entry<String, JsonNode>> linkIt = linksProperties.fields();
        Map<String, LinkDefinition> links = new HashMap<>(linksProperties.size());
        while (linkIt.hasNext()) {
            Entry<String, JsonNode> entry = linkIt.next();
            String linkName = entry.getKey();
            JsonNode linkProperties = entry.getValue()
                                           .get("items")
                                           .get(PROPERTIES);
            LinkDefinition linkDefinition = new LinkDefinition();
            JsonNode attributeProperties = linkProperties.get("attributes")
                                                         .get(PROPERTIES);
            Iterator<Entry<String, JsonNode>> attributeIt = attributeProperties.fields();
            Map<String, Object> attributeSchemas = new HashMap<>(attributeProperties.size());
            while (attributeIt.hasNext()) {
                Entry<String, JsonNode> attributeEntry = attributeIt.next();
                attributeSchemas.put(attributeEntry.getKey(),
                                     OBJECTMAPPER.treeToValue(attributeEntry.getValue(),
                                                              Map.class));
            }
            linkDefinition.setAttributeSchemas(attributeSchemas);

            JsonNode targetProperties = linkProperties.get("target")
                                                      .get(PROPERTIES);

            linkDefinition.setTargetType(targetProperties.get("type")
                                                         .get("enum")
                                                         .get(0)
                                                         .asText());
            linkDefinition.setTargetSubType(targetProperties.get("subType")
                                                            .get("enum")
                                                            .get(0)
                                                            .asText());
            links.put(linkName, linkDefinition);
        }
        return links;
    }

}
