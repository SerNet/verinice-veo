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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
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
    public static final String URL_SOURCE_TYPE = "source-element-type";
    public static final String URL_DESTINATION_TYPE = "destination-element-type";
    public static final String URL_LINK_DEFINITIONS = "link-definitions";
    public static final String URL_PROPERTY_GROUPS = "property-groups";

    @GetMapping(path = "/" + URL_ELEMENT_TYPES)
    public ResponseEntity<Resources<ElementDefinitionResource>>  getElementTypes(){
        List<ElementDefinition> elementDefinitions = new LinkedList<>(getElementDefinitions().values());

        final List<ElementDefinitionResource> collection =
                elementDefinitions.stream().map(ElementDefinitionResource::new).collect(Collectors.toList());

        return ResponseEntity.ok(createResourcesWithSelfLink(collection));
    }

    @GetMapping(path = "/" + URL_ELEMENT_TYPES + "/{elementType}")
    public ResponseEntity<ElementDefinitionResource> getElementType(@PathVariable String elementType){
        ElementDefinition definition = getElementDefinitionFactory().getElementDefinition(elementType);
        return ResponseEntity.ok(new ElementDefinitionResource(definition));
    } 
    
    @RequestMapping(path = URL_ELEMENT_TYPES + "/{elementType}/" + URL_LINK_DEFINITIONS, method = RequestMethod.GET)
    public ResponseEntity<Resources<LinkDefinitionResource>>  getLinkDefinitions(@PathVariable String elementType){
        ElementDefinition definition = getElementDefinitionFactory().getElementDefinition(elementType);
        if(definition==null) {
            new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        Set<LinkDefinition> definitions = 
                getElementDefinitionFactory().getLinkDefinitionsByElementType(elementType);
        final List<LinkDefinitionResource> collection =
                definitions.stream().map(LinkDefinitionResource::new).collect(Collectors.toList());

        return ResponseEntity.ok(createResourcesWithSelfLink(collection));
    }

    @RequestMapping(path = URL_ELEMENT_TYPES + "/{elementType}/" + URL_PROPERTY_GROUPS, method = RequestMethod.GET)
    public ResponseEntity<Resources<PropertyGroupResource>> getPropertyGroups(@PathVariable String elementType){
        ElementDefinition definition = getElementDefinitionFactory().getElementDefinition(elementType);
        if(definition==null) {
            new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        Set<String> groups = getElementDefinitionFactory().getGroupsForElementType(elementType);
        final List<PropertyGroupResource> result = new LinkedList<>();
        for (String group:groups) {
            result.add(new PropertyGroupResource(definition,group));
        }

        return ResponseEntity.ok(createResourcesWithSelfLink(result));
    }

    private <T> Resources<T> createResourcesWithSelfLink(List<T> collection) {
        final Resources<T> resources = new Resources<>(collection);
        final String uriString = ServletUriComponentsBuilder.fromCurrentRequest().build().toUriString();
        resources.add(new Link(uriString, "self"));
        return resources;
    }

    private Map<String, ElementDefinition> getElementDefinitions() {
        return getElementDefinitionFactory().getElementDefinitions();
    }

    private ElementDefinitionFactory getElementDefinitionFactory() {
        return ElementDefinitionFactory.getInstance();
    }

}
