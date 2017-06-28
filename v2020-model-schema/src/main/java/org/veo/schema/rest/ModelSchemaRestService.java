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
 *     Sebastian Hagedorn sh (at) sernet.de - initial API and implementation
 ******************************************************************************/
package org.veo.schema.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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

/**
 * @author sh
 *
 */
@RestController
@RequestMapping("/service/model-schema")
@Service
public class ModelSchemaRestService {
    
    @RequestMapping(path = "/allElementTypes", method = RequestMethod.GET)
    public List<ElementDefinition> getAllElementTypes(){
        List<ElementDefinition> list = new ArrayList<>(ElementDefinitionFactory.getInstance().getElementDefinitions().size());
        list.addAll(ElementDefinitionFactory.getInstance().getElementDefinitions().values());
        HttpStatus status = (list != null && list.size() > 0)
                ? HttpStatus.OK : HttpStatus.NOT_FOUND;
        ResponseEntity<List<ElementDefinition>> response = new ResponseEntity<>(list, status);
        return response.getBody();
    }
    
    @RequestMapping(path = "/elementType/{elementType}", method = RequestMethod.GET)
    public ElementDefinition getElementType(@PathVariable String elementType){
        ElementDefinition definition = ElementDefinitionFactory.getInstance().getElementDefinition(elementType);
        HttpStatus status = (definition != null && definition.getElementType().equals(elementType))
                ? HttpStatus.OK : HttpStatus.NOT_FOUND;
        ResponseEntity<ElementDefinition> response = new ResponseEntity<>(definition, status);
        return response.getBody();
    } 
    
    @RequestMapping(path = "/linkDefinitions/{elementType}", method = RequestMethod.GET)
    public Set<LinkDefinition> getLinkDefinitionsForElementType(@PathVariable String elementType){
        Set<LinkDefinition> definitions = 
                ElementDefinitionFactory.getInstance().getLinkDefinitionsByElementType(elementType);
        HttpStatus status = (definitions != null && definitions.size() > 0)
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
        Set<String> groups = ElementDefinitionFactory.getInstance().getAllGroupNames();
        HttpStatus status = (groups != null && groups.size() > 0)
                ? HttpStatus.OK : HttpStatus.NOT_FOUND;
        ResponseEntity<Set<String>> response = new ResponseEntity<>(groups, status);
        return response.getBody();
    }
    
    @RequestMapping(path = "/propertyGroups/{elementType}", method = RequestMethod.GET)
    public Set<String> getPropertyGroupsByElementType(@PathVariable String elementType){
        Set<String> groups = ElementDefinitionFactory.getInstance().getGroupsForElementType(elementType);
        HttpStatus status = (groups != null && groups.size() > 0)
                ? HttpStatus.OK : HttpStatus.NOT_FOUND;
        ResponseEntity<Set<String>> response = new ResponseEntity<>(groups, status);
        return response.getBody();
    }
    

}
