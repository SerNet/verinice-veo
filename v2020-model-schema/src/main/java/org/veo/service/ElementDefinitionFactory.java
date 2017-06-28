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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.veo.schema.ElementDefinitionResourceLoader;
import org.veo.schema.LinkDefinitionResourceLoader;
import org.veo.schema.model.ElementDefinition;
import org.veo.schema.model.LinkDefinition;
import org.veo.schema.model.LinkDefinitions;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class ElementDefinitionFactory {
    
    private static final ElementDefinitionFactory instance = new ElementDefinitionFactory();
    private Gson gson;
    
    private static final String ELEMENT_DEFINITION_REPOSITORY = "org/veo/model/elementdefinitions/";
    private static final String LINK_DEFINITIONS = "org/veo/model/elementlinks/links.json";
    
    private final Logger logger = LoggerFactory.getLogger(ElementDefinitionFactory.class);
    
    private Map<String, ElementDefinition> elementDefinitionMap;
    private Map<String, Set<LinkDefinition>> linkDefinitionMap;
    
    private ElementDefinitionFactory(){
        elementDefinitionMap = new HashMap<>();
        linkDefinitionMap = new HashMap<>();
        gson = new Gson();
        initElementMap();
        initLinkMap();
    }
    
    public static ElementDefinitionFactory getInstance(){
        return instance;
    }
    

    
    private void initElementMap(){
        try {
            for(File jsonFile : ElementDefinitionResourceLoader.getElementDefinitions()){
                InputStream in = FileUtils.openInputStream(jsonFile);
                String jsonString = IOUtils.toString(in);
                ElementDefinition definition = getElementDefinitonFromJson(jsonString);
                elementDefinitionMap.put(definition.getElementType(), definition);
            }
        } catch (IOException e) {
            logger.error("Error reading json-definition-files from repository", e);
        }
    }
    
    public Set<LinkDefinition> getLinkDefinitionsByElementType(String elementType){
        if (linkDefinitionMap != null && linkDefinitionMap.containsKey(elementType)){
            return linkDefinitionMap.get(elementType);
        } else return Collections.unmodifiableSet(new HashSet<LinkDefinition>(0));
    }
    
    private void initLinkMap(){
        String jsonString;
        try {
            File linkJson = LinkDefinitionResourceLoader.getLinkDefinitionFile();
            InputStream in = FileUtils.openInputStream(linkJson);
            jsonString = IOUtils.toString(in);
            LinkDefinitions definitions = getLinkDefinitionsFromJson(jsonString);
            for (LinkDefinition definition : definitions.getLinkDefinitions()) {
                String source = definition.getSourceType();
                Set<LinkDefinition> linkSet = linkDefinitionMap.get(source);
                if (linkSet == null){
                    linkSet = new HashSet<>();
                } 
                linkSet.add(definition);
                linkDefinitionMap.put(source, linkSet);
                elementDefinitionMap.get(source).addOutgoingLink(definition);
            }
        } catch (IOException e) {
            logger.error("Error reading link-defintion-json-file from repository", e);
        }

    }
    
    
    private LinkDefinitions getLinkDefinitionsFromJson(String json){
        if(isValidJson(json, LinkDefinitions.class)){
            return gson.fromJson(json, LinkDefinitions.class);
        } else {
            LinkDefinitions emptyLinkDefinitions = new LinkDefinitions();
            emptyLinkDefinitions.setLinkDefinitions(Collections.unmodifiableList(new ArrayList<LinkDefinition>(0)));
            return emptyLinkDefinitions;
        }
    }


    
    public Map<String, ElementDefinition> getElementDefinitions(){
        return elementDefinitionMap;
    }
    
    public ElementDefinition getElementDefinition(String elementType){
        if (elementDefinitionMap.containsKey(elementType)) {
            return elementDefinitionMap.get(elementType);
        }
        return null;
    }
    
    private ElementDefinition getElementDefinitonFromJson(String json){
        if(isValidJson(json, ElementDefinition.class)){
            return gson.fromJson(json, ElementDefinition.class);
        } else return null;
    }
    
    private boolean isValidJson(String json, Class<?> clazz){
        try{
            gson.fromJson(json, clazz);
            return true;
        } catch (JsonSyntaxException e) {
            return false;
        }
    }
    
    
    
}
