/*******************************************************************************
 * Copyright (c) 2017 Daniel Murygin.
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
 ******************************************************************************/
package org.veo.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.springframework.lang.NonNull;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 *
 * @author Daniel Murygin
 */
@Entity
public class Link implements Serializable {

    @Id
    @Column(length = 36)
    private String uuid;

    @Column(length = 255)
    private String title;

    @Column(nullable = false, length = 255)
    private String typeId;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    private Element source;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    private Element destination;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "link_uuid")
    private Set<LinkProperty> properties;

    public Link() {
        this(UUID.randomUUID().toString());
    }

    public Link(@NonNull String uuid) {
        this.uuid = uuid;
        properties = new HashSet<>();
    }

    /**
     * Returns a unmodifiable map with all properties of this element. Key of the
     * map is the key of the property.
     *
     * @return A unmodifiable map with all properties
     */
    @JsonIgnore
    public Map<String, List<LinkProperty>> getPropertyMap() {
        Map<String, List<LinkProperty>> propertyMap = new HashMap<>(getProperties().size());
        for (LinkProperty property : getProperties()) {
            List<LinkProperty> propertyList = propertyMap.get(property.getKey());
            if (propertyList == null) {
                // The cardinality of most properties is single
                propertyList = new ArrayList<>(1);
                propertyMap.put(property.getKey(), propertyList);
            }
            propertyList.add(property);
        }
        return Collections.unmodifiableMap(propertyMap);
    }

    public String getUuid() {
        return uuid;
    }

    public String getTitle() {
        return title;
    }

    public Element getSource() {
        return source;
    }

    public Element getDestination() {
        return destination;
    }

    public Set<LinkProperty> getProperties() {
        if (properties == null) {
            properties = new HashSet<>();
        }
        return properties;
    }

    public String getTypeId() {
        return typeId;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setSource(Element source) {
        this.source = source;
    }

    public void setDestination(Element destination) {
        this.destination = destination;
    }

    public void setProperties(Set<LinkProperty> properties) {
        this.properties = properties;
    }

    public void setTypeId(String typeId) {
        this.typeId = typeId;
    }

    public void addProperty(LinkProperty property) {
        getProperties().add(property);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((uuid == null) ? 0 : uuid.hashCode());
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
        Link other = (Link) obj;
        if (uuid == null) {
            if (other.uuid != null) {
                return false;
            }
        } else if (!uuid.equals(other.uuid)) {
            return false;
        }
        return true;
    }
}
