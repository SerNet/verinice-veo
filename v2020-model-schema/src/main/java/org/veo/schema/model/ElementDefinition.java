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
package org.veo.schema.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class ElementDefinition {
    
    private int id;
    private int parentId;
    
    private List<String> tags;
    
    private String elementType;
    
    private List<PropertyDefinition> properties;   
    
    private List<LinkDefinition> outgoingLinks;
    
    public void addProperty(PropertyDefinition property){
        properties.add(property);
    }
    
    public PropertyDefinition getProperty(String type){
        for (PropertyDefinition property : properties){
            if (type.equals(property.getType())){
                return property;
            }
        }
        return null;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getParentId() {
        return parentId;
    }

    public void setParentId(int parentId) {
        this.parentId = parentId;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getElementType() {
        return elementType;
    }

    public void setElementType(String elementType) {
        this.elementType = elementType;
    }

    public List<PropertyDefinition> getProperties() {
        return properties;
    }

    public void setProperties(List<PropertyDefinition> properties) {
        this.properties = properties;
    }

    public List<LinkDefinition> getOutgoingLinks() {
        return outgoingLinks;
    }

    public void setOutgoingLinks(List<LinkDefinition> outgoingLinks) {
        this.outgoingLinks = outgoingLinks;
    }
    
    public void addOutgoingLink(LinkDefinition linkDefinition) {
        if(this.outgoingLinks == null){
            outgoingLinks = new ArrayList<>(1024);
        }
        outgoingLinks.add(linkDefinition);
    }
    
}
