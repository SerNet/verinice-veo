/*******************************************************************************
 * Copyright (c) 2017 Sebastian Hagedorn.
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
 *
 * Contributors:
 *     Sebastian Hagedorn sh (at) sernet.de - initial API and implementation
 ******************************************************************************/
package org.veo.schema;

import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.veo.schema.model.ElementDefinition;
import org.veo.schema.model.LinkDefinition;
import org.veo.schema.model.LinkDefinitions;
import org.veo.schema.model.PropertyDefinition;

import java.io.IOException;
import java.util.*;

/**
 * Load all element and links definitions from json files
 * in folders elementdefinitions and linkdefinitions.
 *
 * Usage:
 * ElementDefinitionFactory.getInstance()..getElementDefinitions();
 * ElementDefinitionFactory.getInstance()..getElementDefinition("asset");
 */
public class ElementDefinitionFactory {

    private final Logger LOG = LoggerFactory.getLogger(ElementDefinitionFactory.class);

    private static final String ELEMENT_DEFINITION_DIR_NAME = "elementdefinitions";
    private static final String LINK_DEFINITION_DIR_NAME = "linkdefinitions";
    private static final String JSON_FILE_EXTENSION = "json";

    private static final ElementDefinitionFactory instance = new ElementDefinitionFactory();
    private ObjectMapper jsonObjectMapper;

    private Map<String, ElementDefinition> elementDefinitionMap;
    private Map<String, Set<LinkDefinition>> linkDefinitionMap;

    private ElementDefinitionFactory() {
        elementDefinitionMap = new HashMap<>();
        linkDefinitionMap = new HashMap<>();
        try {
            initJsonMapper();
            initElementMap();
            initLinkMap();
        } catch (IOException e) {
            LOG.error("Error while reading element definitions", e);
        }
    }

    public static ElementDefinitionFactory getInstance() {
        return instance;
    }

    private void initJsonMapper() {
        jsonObjectMapper = new ObjectMapper();
        jsonObjectMapper.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        jsonObjectMapper.enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES);
        jsonObjectMapper.enable(MapperFeature.USE_ANNOTATIONS);
        jsonObjectMapper.configure(Feature.ALLOW_MISSING_VALUES, false);
        jsonObjectMapper.configure(Feature.IGNORE_UNDEFINED, false);
        jsonObjectMapper.enable(SerializationFeature.FAIL_ON_UNWRAPPED_TYPE_IDENTIFIERS);
        jsonObjectMapper.enable(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES);
        jsonObjectMapper.enable(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE);
        jsonObjectMapper.enable(DeserializationFeature.FAIL_ON_UNRESOLVED_OBJECT_IDS);
        jsonObjectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    private void initElementMap() throws IOException {
        for (String jsonFile : loadElementDefinitions()) {
            handleElementDefinition(jsonFile);
        }
    }

    private void handleElementDefinition(String jsonString) throws IOException {
        ElementDefinition definition = getElementDefinitionFromJson(jsonString);
        if(definition!=null) {
            elementDefinitionMap.put(definition.getElementType(), definition);
        }
    }

    private void initLinkMap() throws IOException {
        for (String jsonFile : loadLinkDefinitions()) {
            handleLinkDefinitions(jsonFile);
        }
    }

    private void handleLinkDefinitions(String jsonString) throws IOException {
        LinkDefinitions definitions = getLinkDefinitionsFromJson(jsonString);
        for (LinkDefinition definition : definitions.getLinkDefinitions()) {
            handleLinkDefinition(definition);
        }
    }

    private LinkDefinitions getLinkDefinitionsFromJson(String json) throws IOException {
        if (isValidJson(json, LinkDefinitions.class)) {
            return jsonObjectMapper.readValue(json, LinkDefinitions.class);
        } else {
            LinkDefinitions emptyLinkDefinitions = new LinkDefinitions();
            emptyLinkDefinitions.setLinkDefinitions(Collections.emptyList());
            return emptyLinkDefinitions;
        }
    }

    private void handleLinkDefinition(LinkDefinition definition) {
        String source = definition.getSourceType();
        Set<LinkDefinition> linkSet = linkDefinitionMap.get(source);
        if (linkSet == null) {
            linkSet = new HashSet<>();
        }
        linkSet.add(definition);
        linkDefinitionMap.put(source, linkSet);
        elementDefinitionMap.get(source).addOutgoingLink(definition);
    }

    public Set<LinkDefinition> getLinkDefinitionsByElementType(String elementType) {
        if (linkDefinitionMap != null && linkDefinitionMap.containsKey(elementType)) {
            return linkDefinitionMap.get(elementType);
        } else
            return Collections.emptySet();
    }

    public Set<String> getGroupsForElementType(String elementType) {
        Set<String> groups = new HashSet<>(128);
        if (elementDefinitionMap.containsKey(elementType)) {
            for (PropertyDefinition propertyDefinition : elementDefinitionMap.get(elementType).getProperties()) {
                groups.add(propertyDefinition.getGroup());
            }
        }
        return groups;
    }

    public Map<String, ElementDefinition> getElementDefinitions() {
        return elementDefinitionMap;
    }

    public ElementDefinition getElementDefinition(String elementType) {
        if (elementDefinitionMap.containsKey(elementType)) {
            return elementDefinitionMap.get(elementType);
        }
        return null;
    }

    private ElementDefinition getElementDefinitionFromJson(String json) throws IOException {
        if (isValidJson(json, ElementDefinition.class)) {
            return jsonObjectMapper.readValue(json, ElementDefinition.class);
        } else
            return null;
    }

    private boolean isValidJson(String json, Class<?> clazz) {
        final String WARN_MSG = "Failed to parse json:\n";
        try {
            jsonObjectMapper.readValue(json, clazz);
            return true;
        } catch (IOException e) {
            LOG.warn(WARN_MSG, e);
            return false;
        }
    }

    private Set<String> loadElementDefinitions(){
        return ClasspathResourceLoader.loadResources(ELEMENT_DEFINITION_DIR_NAME, JSON_FILE_EXTENSION);
    }

    private Set<String> loadLinkDefinitions(){
        return ClasspathResourceLoader.loadResources(LINK_DEFINITION_DIR_NAME, JSON_FILE_EXTENSION);
    }

}
