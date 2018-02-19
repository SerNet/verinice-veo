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
@RequestMapping(ModelSchemaRestService.URL_SERVICE)
@Service
public class ModelSchemaRestService {

    public static final String URL_SERVICE = "/service/model-schema";
    public static final String URL_ELEMENT_TYPES = "element-types";
    public static final String URL_LINK_DEFINITIONS = "/link-definitions";
    public static final String URL_PROPERTY_GROUPS = "/property-groups";

    @RequestMapping(path = "/" + URL_ELEMENT_TYPES, method = RequestMethod.GET)
    public ResponseEntity<List<ElementDefinition>> getElementTypes(){
        List<ElementDefinition> elementDefinitions = new ArrayList<>(getElementDefinitions().size());
        elementDefinitions.addAll(getElementDefinitions().values());
        HttpStatus status = isNotEmpty(elementDefinitions)
                ? HttpStatus.OK : HttpStatus.NOT_FOUND;
        return new ResponseEntity<>(elementDefinitions, status);
    }

    @RequestMapping(path = "/" + URL_ELEMENT_TYPES + "/{elementType}", method = RequestMethod.GET)
    public ResponseEntity<ElementDefinition> getElementType(@PathVariable String elementType){
        ElementDefinition definition = getElementDefinitionFactory().getElementDefinition(elementType);
        HttpStatus status = (definition != null && definition.getElementType().equals(elementType))
                ? HttpStatus.OK : HttpStatus.NOT_FOUND;
        return new ResponseEntity<>(definition, status);
    } 
    
    @RequestMapping(path = URL_ELEMENT_TYPES + "/{elementType}/" + URL_LINK_DEFINITIONS, method = RequestMethod.GET)
    public ResponseEntity<Set<LinkDefinition>> getLinkDefinitions(@PathVariable String elementType){
        Set<LinkDefinition> definitions = 
                getElementDefinitionFactory().getLinkDefinitionsByElementType(elementType);
        HttpStatus status = isNotEmpty(definitions)
                ? HttpStatus.OK : HttpStatus.NOT_FOUND;
        return new ResponseEntity<>(definitions, status);
    }
    
    @RequestMapping(path = URL_PROPERTY_GROUPS, method = RequestMethod.GET)
    public ResponseEntity<Set<String>> getPropertyGroups(){
        Set<String> groups = getElementDefinitionFactory().getAllGroupNames();
        HttpStatus status = isNotEmpty(groups)
                ? HttpStatus.OK : HttpStatus.NOT_FOUND;
        return new ResponseEntity<>(groups, status);
    }
    
    @RequestMapping(path = URL_ELEMENT_TYPES + "/{elementType}/" + URL_PROPERTY_GROUPS, method = RequestMethod.GET)
    public ResponseEntity<Set<String>> getPropertyGroups(@PathVariable String elementType){
        Set<String> groups = getElementDefinitionFactory().getGroupsForElementType(elementType);
        HttpStatus status = isNotEmpty(groups)
                ? HttpStatus.OK : HttpStatus.NOT_FOUND;
        return new ResponseEntity<>(groups, status);
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
