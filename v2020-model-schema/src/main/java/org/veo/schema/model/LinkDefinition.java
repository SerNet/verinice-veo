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
package org.veo.schema.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.List;

/**
 * The definition of an schema link. A link always
 * has a source and a destination element type any number of additional
 * properties.
 *
 * @author Sebastian Hagedorn
 */
public class LinkDefinition implements Serializable {

    private static final long serialVersionUID = 20170629135211L;

    private String sourceType;

    private String destinationType;

    private List<PropertyDefinition> properties;

    @JsonCreator
    public LinkDefinition(
            @JsonProperty(value = "sourceType",
                    required = true) final String sourceType,
            @JsonProperty(value = "destinationType",
                    required = true) final String destinationType,
            @JsonProperty(value = "properties",
                    required = true) final List<PropertyDefinition> properties
    ) {

        this.sourceType = sourceType;
        this.destinationType = destinationType;
        this.properties = properties;

    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getDestinationType() {
        return destinationType;
    }

    public void setDestinationType(String destinationType) {
        this.destinationType = destinationType;
    }

    public List<PropertyDefinition> getProperties() {
        return properties;
    }

    public void setProperties(List<PropertyDefinition> properties) {
        this.properties = properties;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((destinationType == null) ? 0 : destinationType.hashCode());
        result = prime * result + ((properties == null) ? 0 : properties.hashCode());
        result = prime * result + ((sourceType == null) ? 0 : sourceType.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        LinkDefinition other = (LinkDefinition) obj;
        if (destinationType == null) {
            if (other.destinationType != null)
                return false;
        } else if (!destinationType.equals(other.destinationType))
            return false;
        if (properties == null) {
            if (other.properties != null)
                return false;
        } else if (!properties.equals(other.properties))
            return false;
        if (sourceType == null) {
            if (other.sourceType != null)
                return false;
        } else if (!sourceType.equals(other.sourceType))
            return false;
        return true;
    }


}
