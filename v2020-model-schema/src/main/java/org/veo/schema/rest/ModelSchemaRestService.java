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

import org.springframework.hateoas.Link;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.hateoas.Resources;
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
import java.util.stream.Collectors;

/**
 * REST service to access the schema for the elements.
 * The schema defines the element, property and link types of elements.
 *
 * @author Sebastian Hagedorn
 * @author Daniel Murygin
 */
@RestController
@RequestMapping(value = ModelSchemaRestService.URL_SERVICE, produces = "application/hal+json")
public class ModelSchemaRestService {

    public static final String URL_SERVICE = "/service/model-schema";
    public static final String URL_ELEMENT_TYPES = "element-types";
    public static final String URL_LINK_DEFINITIONS = "/link-definitions";
    public static final String URL_PROPERTY_GROUPS = "/property-groups";

    @GetMapping(path = "/" + URL_ELEMENT_TYPES)
    public ResponseEntity<Resources<ElementDefinitionResource>>  getElementTypes(){
        List<ElementDefinition> elementDefinitions = new LinkedList<>(getElementDefinitions().values());

        final List<ElementDefinitionResource> collection =
                elementDefinitions.stream().map(ElementDefinitionResource::new).collect(Collectors.toList());

        final Resources<ElementDefinitionResource> resources = new Resources<>(collection);
        final String uriString = ServletUriComponentsBuilder.fromCurrentRequest().build().toUriString();
        resources.add(new Link(uriString, "self"));
        return ResponseEntity.ok(resources);
    }

    @GetMapping(path = "/" + URL_ELEMENT_TYPES + "/{elementType}")
    public ResponseEntity<ElementDefinitionResource> getElementType(@PathVariable String elementType){
        ElementDefinition definition = getElementDefinitionFactory().getElementDefinition(elementType);
        return ResponseEntity.ok(new ElementDefinitionResource(definition));
    } 
    
    @RequestMapping(path = URL_ELEMENT_TYPES + "/{elementType}/" + URL_LINK_DEFINITIONS, method = RequestMethod.GET)
    public Set<LinkDefinition> getLinkDefinitions(@PathVariable String elementType){
        Set<LinkDefinition> definitions = 
                getElementDefinitionFactory().getLinkDefinitionsByElementType(elementType);
        HttpStatus status = isNotEmpty(definitions)
                ? HttpStatus.OK : HttpStatus.NOT_FOUND;
        ResponseEntity<Set<LinkDefinition>> response = new ResponseEntity<>(definitions, status);
        return response.getBody();        
    }
    
    @RequestMapping(path = URL_PROPERTY_GROUPS, method = RequestMethod.GET)
    public Set<String> getPropertyGroups(){
        Set<String> groups = getElementDefinitionFactory().getAllGroupNames();
        HttpStatus status = isNotEmpty(groups)
                ? HttpStatus.OK : HttpStatus.NOT_FOUND;
        ResponseEntity<Set<String>> response = new ResponseEntity<>(groups, status);
        return response.getBody();
    }
    
    @RequestMapping(path = URL_ELEMENT_TYPES + "/{elementType}/" + URL_PROPERTY_GROUPS, method = RequestMethod.GET)
    public Set<String> getPropertyGroups(@PathVariable String elementType){
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



    static class ElementDefinitionResource extends ResourceSupport {
        private ElementDefinition elementDefinition;

        public ElementDefinitionResource(ElementDefinition elementDefinition) {
            this.elementDefinition = elementDefinition;
        }
    }
}
