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
 * Resource DTO class for returning a property group in a REST service result.
 *
 * @author Daniel Murygin
 */
public class PropertyGroupResource extends ResourceSupport {

    private String group;

    @JsonCreator
    public PropertyGroupResource(ElementDefinition elementDefinition, @JsonProperty("content") String group) {
        if (group == null) {
            return;
        }
        this.group = group;
        final String type = elementDefinition.getElementType();
        add(linkTo(methodOn(ModelSchemaRestService.class).getElementType(type)).withRel(ModelSchemaRestService.URL_SOURCE_TYPE));
    }

    public String getGroup() {
        return group;
    }

}