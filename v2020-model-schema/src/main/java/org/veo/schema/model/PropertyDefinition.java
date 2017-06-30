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

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class PropertyDefinition {
    
    public static final String DEFAULT_GROUP_NAME = "default_group";
    
    public enum PropertyType{
        LABEL,  // one row
        TEXT,   // more than one row
        NUMBER,
        SINGLEOPTION,
        MULTIOPTION,
        DATE,
        BOOLEAN
    }
    
    private String name;
    
    private PropertyType type;
    
    private List<PropertyOptionDefinition> propertyOptionDefinitions;
    
    private String group;
    
    @JsonCreator
    public PropertyDefinition(
            @JsonProperty(value= "name", 
                required = true) final String name,
            @JsonProperty(value= "type",
                required = true) final PropertyType type,
            @JsonProperty(value= "propertyOptionDefinitions",
                required = false) final List<PropertyOptionDefinition> propertyOptionDefinition,
            @JsonProperty(value= "group",
                required = false) final String group
            ){
        this.name = name;
        this.type = type;
        this.propertyOptionDefinitions = propertyOptionDefinition;
        this.group = group;
        
    }
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public PropertyType getType() {
        return type;
    }

    public void setType(PropertyType type) {
        this.type = type;
    }

    public List<PropertyOptionDefinition> getPropertyOptionDefinitons() {
        return propertyOptionDefinitions;
    }

    public void setPropertyOptionDefinitons(List<PropertyOptionDefinition> propertyOptionDefinitons) {
        this.propertyOptionDefinitions = propertyOptionDefinitons;
    }

    public String getGroup() {
        if (group == null || group.length() == 0){
            return DEFAULT_GROUP_NAME;
        }
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }
    
    
    
}
