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
package org.veo.service;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.databind.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.veo.schema.ElementDefinitionResourceLoader;
import org.veo.schema.LinkDefinitionResourceLoader;
import org.veo.schema.model.ElementDefinition;
import org.veo.schema.model.LinkDefinition;
import org.veo.schema.model.LinkDefinitions;
import org.veo.schema.model.PropertyDefinition;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ElementDefinitionFactory {

    private final Logger LOG = LoggerFactory.getLogger(ElementDefinitionFactory.class);

    private static final ElementDefinitionFactory instance = new ElementDefinitionFactory();
    private ObjectMapper mapper;


    private Map<String, ElementDefinition> elementDefinitionMap;
    private Map<String, Set<LinkDefinition>> linkDefinitionMap;

    private ElementDefinitionFactory() {
        elementDefinitionMap = new HashMap<>();
        linkDefinitionMap = new HashMap<>();
        initJsonMapper();
        initElementMap();
        initLinkMap();
    }

    public static ElementDefinitionFactory getInstance() {
        return instance;
    }

    private void initJsonMapper() {
        mapper = new ObjectMapper();
        mapper.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES);
        mapper.enable(MapperFeature.USE_ANNOTATIONS);
        mapper.configure(Feature.ALLOW_MISSING_VALUES, false);
        mapper.configure(Feature.IGNORE_UNDEFINED, false);
        mapper.enable(SerializationFeature.FAIL_ON_UNWRAPPED_TYPE_IDENTIFIERS);
        mapper.enable(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES);
        mapper.enable(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE);
        mapper.enable(DeserializationFeature.FAIL_ON_UNRESOLVED_OBJECT_IDS);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    private void initElementMap() {
        for (File jsonFile : ElementDefinitionResourceLoader.getElementDefinitions()) {
            handleElementDefinition(jsonFile);
        }
    }

    private void handleElementDefinition(File jsonFile) {
        try (InputStream in = FileUtils.openInputStream(jsonFile)) {
            String jsonString = IOUtils.toString(in, StandardCharsets.UTF_8);
            if (isValidJson(jsonString, ElementDefinition.class)) {
                ElementDefinition definition = getElementDefinitonFromJson(jsonString);
                if(definition!=null) {
                    elementDefinitionMap.put(definition.getElementType(), definition);
                }
            }
        } catch (IOException e) {
            LOG.error("Error while loading element definition file: {}", jsonFile.getPath(), e);
        }
    }

    private void initLinkMap() {
        List<File> linkJsonList = LinkDefinitionResourceLoader.getLinkDefinitionFile();
        for (File jsonFile : linkJsonList) {
            handleLinkDefinitionFile(jsonFile);
        }
    }

    private void handleLinkDefinitionFile(File jsonFile) {
        try {
            InputStream in = FileUtils.openInputStream(jsonFile);
            String jsonString = IOUtils.toString(in, StandardCharsets.UTF_8);
            LinkDefinitions definitions = getLinkDefinitionsFromJson(jsonString);
            for (LinkDefinition definition : definitions.getLinkDefinitions()) {
                handleLinkDefinition(definition);
            }
        } catch (IOException e) {
            LOG.error("Error while loading link definition file: {}", jsonFile.getPath(), e);
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

    private LinkDefinitions getLinkDefinitionsFromJson(String json) throws JsonParseException, JsonMappingException, IOException {
        if (isValidJson(json, LinkDefinitions.class)) {
            return mapper.readValue(json, LinkDefinitions.class);
        } else {
            LinkDefinitions emptyLinkDefinitions = new LinkDefinitions();
            emptyLinkDefinitions.setLinkDefinitions(Collections.emptyList());
            return emptyLinkDefinitions;
        }
    }

    public Set<String> getAllGroupNames() {
        Set<String> groups = new HashSet<>(128);
        for (ElementDefinition elementDefinition : getElementDefinitions().values()) {
            groups.addAll(getGroupsForElementDefinition(elementDefinition));
        }
        return groups;
    }

    public Set<String> getGroupsForElementDefinition(ElementDefinition elementDefinition) {
        return getGroupsForElementType(elementDefinition.getElementType());
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

    private ElementDefinition getElementDefinitonFromJson(String json) throws JsonParseException, JsonMappingException, IOException {
        if (isValidJson(json, ElementDefinition.class)) {
            return mapper.readValue(json, ElementDefinition.class);
        } else
            return null;
    }

    private boolean isValidJson(String json, Class<?> clazz) {
        final String WARN_MSG = "Failed to parse json:\n";
        try {
            mapper.readValue(json, clazz);
            return true;
        } catch (IOException e) {
            LOG.warn(WARN_MSG, e);
            return false;
        }
    }

}
