/*******************************************************************************
 * Copyright (c) 2017 Sebastian Hagedorn
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
 *     Sebastian Hagedorn - initial API and implementation
 *     Daniel Murygin
 ******************************************************************************/
package org.veo.schema.rest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.veo.schema.model.ElementDefinition;
import org.veo.schema.model.LinkDefinition;
import org.veo.service.ElementDefinitionFactory;

import java.util.*;

/**
 * REST service to access the schema for the elements.
 * The schema defines the element, property and link types of elements.
 *
 * @author Sebastian Hagedorn
 * @author Daniel Murygin
 */
@RestController
@RequestMapping("/service/model-schema")
@Service
public class ModelSchemaRestService {

    @RequestMapping(path = "/allElementTypes", method = RequestMethod.GET)
    public List<ElementDefinition> getAllElementTypes(){
        List<ElementDefinition> elementDefinitions = new ArrayList<>(getElementDefinitions().size());
        elementDefinitions.addAll(getElementDefinitions().values());
        HttpStatus status = isNotEmpty(elementDefinitions)
                ? HttpStatus.OK : HttpStatus.NOT_FOUND;
        ResponseEntity<List<ElementDefinition>> response = new ResponseEntity<>(elementDefinitions, status);
        return response.getBody();
    }

    @RequestMapping(path = "/elementType/{elementType}", method = RequestMethod.GET)
    public ElementDefinition getElementType(@PathVariable String elementType){
        ElementDefinition definition = getElementDefinitionFactory().getElementDefinition(elementType);
        HttpStatus status = (definition != null && definition.getElementType().equals(elementType))
                ? HttpStatus.OK : HttpStatus.NOT_FOUND;
        ResponseEntity<ElementDefinition> response = new ResponseEntity<>(definition, status);
        return response.getBody();
    } 
    
    @RequestMapping(path = "/linkDefinitions/{elementType}", method = RequestMethod.GET)
    public Set<LinkDefinition> getLinkDefinitionsForElementType(@PathVariable String elementType){
        Set<LinkDefinition> definitions = 
                getElementDefinitionFactory().getLinkDefinitionsByElementType(elementType);
        HttpStatus status = isNotEmpty(definitions)
                ? HttpStatus.OK : HttpStatus.NOT_FOUND;
        ResponseEntity<Set<LinkDefinition>> response = new ResponseEntity<>(definitions, status);
        return response.getBody();        
    }
    
    @RequestMapping(path = "/propertyGroups/{elementDefinition}", method = RequestMethod.GET)
    public Set<String> getGroupsForElementDefinition(ElementDefinition elementDefinition){
        return getPropertyGroupsByElementType(elementDefinition.getElementType());
    }
    
    @RequestMapping(path = "/allPropertyGroups", method = RequestMethod.GET)
    public Set<String> getAllPropertyGroups(){
        Set<String> groups = getElementDefinitionFactory().getAllGroupNames();
        HttpStatus status = isNotEmpty(groups)
                ? HttpStatus.OK : HttpStatus.NOT_FOUND;
        ResponseEntity<Set<String>> response = new ResponseEntity<>(groups, status);
        return response.getBody();
    }
    
    @RequestMapping(path = "/propertyGroups/{elementType}", method = RequestMethod.GET)
    public Set<String> getPropertyGroupsByElementType(@PathVariable String elementType){
        Set<String> groups = getElementDefinitionFactory().getGroupsForElementType(elementType);
        HttpStatus status = isNotEmpty(groups)
                ? HttpStatus.OK : HttpStatus.NOT_FOUND;
        ResponseEntity<Set<String>> response = new ResponseEntity<>(groups, status);
        return response.getBody();
    }


    private Map<String, ElementDefinition> getElementDefinitions() {
        return getElementDefinitionFactory().getElementDefinitions();
    }

    private ElementDefinitionFactory getElementDefinitionFactory() {
        return ElementDefinitionFactory.getInstance();
    }

    private boolean isNotEmpty(Collection<?> elementDefinitions) {
        return elementDefinitions != null && !elementDefinitions.isEmpty();
    }

}
