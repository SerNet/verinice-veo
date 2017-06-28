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

public class PropertyDefinition {
    
    public enum PropertyType{
        LABEL,  // one row
        TEXT,   // more than one row
        NUMBER,
        SINGLEOPTION,
        MULTIOPTION,
        DATE,
        BOOLEAN
    }
    
    String name;
    PropertyType type;
    List<PropertyOptionDefinition> propertyOptionDefinitions;

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
    
    
    
}
