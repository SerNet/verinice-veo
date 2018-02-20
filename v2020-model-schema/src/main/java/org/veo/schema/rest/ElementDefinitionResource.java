/*******************************************************************************
 * Copyright (c) 2018 Daniel Murygin
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
 *     Daniel Murygin - initial API and implementation
 ******************************************************************************/
package org.veo.schema.rest;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.hateoas.ResourceSupport;
import org.veo.schema.model.ElementDefinition;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

/**
 * Resource DTO class for returning a element definition in a REST service result.
 *
 * @author Daniel Murygin
 */
public class ElementDefinitionResource extends ResourceSupport {

    private ElementDefinition elementDefinition;

    @JsonCreator
    public ElementDefinitionResource(@JsonProperty("content") ElementDefinition elementDefinition) {
        if (elementDefinition == null) {
            return;
        }
        this.elementDefinition = elementDefinition;
        final String type = elementDefinition.getElementType();
        add(linkTo(methodOn(ModelSchemaRestService.class).getElementType(type)).withSelfRel());
        add(linkTo(methodOn(ModelSchemaRestService.class).getLinkDefinitions(type)).withRel(ModelSchemaRestService.URL_LINK_DEFINITIONS));
        add(linkTo(methodOn(ModelSchemaRestService.class).getPropertyGroups(type)).withRel(ModelSchemaRestService.URL_PROPERTY_GROUPS));
    }

    public ElementDefinition getElementDefinition() {
        return elementDefinition;
    }

}