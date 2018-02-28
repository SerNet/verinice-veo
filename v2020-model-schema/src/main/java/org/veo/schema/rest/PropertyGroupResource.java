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

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

import org.springframework.hateoas.ResourceSupport;
import org.veo.schema.model.ElementDefinition;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        PropertyGroupResource that = (PropertyGroupResource) o;

        return group != null ? group.equals(that.group) : that.group == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (group != null ? group.hashCode() : 0);
        return result;
    }
}