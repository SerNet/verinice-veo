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
 *     Sebastian Hagedorn - initial API and implementation
 ******************************************************************************/
package org.veo.schema.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * The definition of an schema element or rather an entity. An element always
 * has a type ID, standard and any number of additional properties. Any links to
 * other elements can be defined for an element.
 *
 * @author Sebastian Hagedorn
 */
public class ElementDefinition implements Serializable {

    private static final long serialVersionUID = 20170629135134L;

    private String elementType;

    private Set<PropertyDefinition> properties;

    private Set<LinkDefinition> outgoingLinks;

    @JsonCreator
    public ElementDefinition(
            @JsonProperty(value= "elementType",
                required = true) final String elementType,
            @JsonProperty(value= "properties",
            required = true) final Set<PropertyDefinition> properties
            ){
        this.properties = new HashSet<>();
        properties.addAll(createDefaultProperties());
        this.elementType = elementType;
        this.properties.addAll(properties);
    }

    /**
     * every tree element of veo has to provide the three properties
     * - id (primary key of the element, not null)
     * - parentId (primary key of the parent element, not null)
     * - tags (tags of element, nullable)
     * @return
     */
    private Set<PropertyDefinition> createDefaultProperties(){
        Set<PropertyDefinition> defaultProperties = new HashSet<>(3);
        PropertyDefinition definition = new PropertyDefinition("id",
                PropertyDefinition.PropertyType.NUMBER, null, null);
        defaultProperties.add(definition);
        definition = new PropertyDefinition("parentId",
                PropertyDefinition.PropertyType.NUMBER, null, null);
        defaultProperties.add(definition);
        definition = new PropertyDefinition("tags",
                PropertyDefinition.PropertyType.LABEL, null, null);
        defaultProperties.add(definition);
        return defaultProperties;
    }

    public void addProperty(PropertyDefinition property){
        properties.add(property);
    }

    public PropertyDefinition getProperty(String name){
        for (PropertyDefinition property : properties){
            if (name.equals(property.getName())){
                return property;
            }
        }
        return null;
    }

    public String getElementType() {
        return elementType;
    }

    public void setElementType(String elementType) {
        this.elementType = elementType;
    }

    public Set<PropertyDefinition> getProperties() {
        return properties;
    }

    public void setProperties(Set<PropertyDefinition> properties) {
        this.properties = properties;
    }

    public Set<LinkDefinition> getOutgoingLinks() {
        return outgoingLinks;
    }

    public void setOutgoingLinks(Set<LinkDefinition> outgoingLinks) {
        this.outgoingLinks = outgoingLinks;
    }

    public void addOutgoingLink(LinkDefinition linkDefinition) {
        if(this.outgoingLinks == null){
            outgoingLinks = new HashSet<>(1024);
        }
        outgoingLinks.add(linkDefinition);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((elementType == null) ? 0 : elementType.hashCode());
        result = prime * result + ((properties == null) ? 0 : properties.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ElementDefinition other = (ElementDefinition) obj;
        if (elementType == null) {
            if (other.elementType != null) {
                return false;
            }
        } else if (!elementType.equals(other.elementType)) {
            return false;
        }
        if (properties == null) {
            if (other.properties != null) {
                return false;
            }
        } else if (!properties.equals(other.properties)) {
            return false;
        }
        return true;
    }
}
