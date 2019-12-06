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
import javax.persistence.NamedAttributeNode;
import javax.persistence.NamedEntityGraph;
import javax.persistence.NamedEntityGraphs;
import javax.persistence.NamedSubgraph;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;

import org.springframework.lang.NonNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * @author Daniel Murygin
 */
@Entity
@NamedEntityGraphs({
        @NamedEntityGraph(name = "linksWithProperties",
                          attributeNodes = {
                                  @NamedAttributeNode(value = "linksOutgoing",
                                                      subgraph = "linksGraph"),
                                  @NamedAttributeNode(value = "properties") },
                          subgraphs = {
                                  @NamedSubgraph(name = "linksGraph",
                                                 attributeNodes = {
                                                         @NamedAttributeNode(value = "properties") }) }),
        @NamedEntityGraph(name = "properties",
                          attributeNodes = { @NamedAttributeNode(value = "properties") }) })
@JsonInclude(Include.NON_NULL)
public class Element implements Serializable {

    @Id
    @Column(length = 36)
    private String uuid;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, length = 255)
    private String typeId;

    @ManyToOne
    @JoinColumn(name = "scope_uuid")
    @JsonIgnore
    private Element scope;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "parent_uuid")
    @JsonIgnore
    private Element parent;

    @OneToMany(cascade = CascadeType.ALL)
    @OrderColumn
    @JoinColumn(name = "parent_uuid")
    @JsonIgnore
    private Set<Element> children = new HashSet<>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "source")
    @JsonIgnore
    private Set<Link> linksOutgoing = new HashSet<>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "destination")
    @JsonIgnore
    private Set<Link> linksIncoming = new HashSet<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "element_uuid")
    private Set<ElementProperty> properties;

    public Element() {
        this(UUID.randomUUID()
                 .toString());
    }

    public Element(@NonNull String uuid) {
        this.uuid = uuid;
        properties = new HashSet<>();
    }

    public String getUuid() {
        return uuid;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTypeId() {
        return typeId;
    }

    public void setTypeId(String typeId) {
        this.typeId = typeId;
    }

    public Element getParent() {
        return parent;
    }

    public void setParent(Element parent) {
        this.parent = parent;
    }

    public Set<Element> getChildren() {
        return children;
    }

    public void setChildren(Set<Element> children) {
        this.children = children;
    }

    public Set<Link> getLinksOutgoing() {
        return linksOutgoing;
    }

    public void setLinksOutgoing(Set<Link> linksOutgoing) {
        this.linksOutgoing = linksOutgoing;
    }

    public Set<Link> getLinksIncoming() {
        return linksIncoming;
    }

    public void setLinksIncoming(Set<Link> linksIncoming) {
        this.linksIncoming = linksIncoming;
    }

    @JsonIgnore
    public Set<Element> getLinkedDestinations() {
        Set<Element> linkedElement = new HashSet<>();
        if (getLinksOutgoing() != null) {
            for (Link link : getLinksOutgoing()) {
                linkedElement.add(link.getDestination());
            }
        }
        return linkedElement;
    }

    @JsonIgnore
    public Set<Element> getLinkedSources() {
        Set<Element> linkedElement = new HashSet<>();
        if (getLinksIncoming() != null) {
            for (Link link : getLinksIncoming()) {
                linkedElement.add(link.getSource());
            }
        }
        return linkedElement;
    }

    public Element getScope() {
        return scope;
    }

    public void setScope(Element scope) {
        this.scope = scope;
    }

    public Set<ElementProperty> getProperties() {
        if (properties == null) {
            properties = new HashSet<>();
        }
        return properties;
    }

    public void setProperties(Set<ElementProperty> properties) {
        this.properties = properties;
    }

    public void addChild(Element children) {
        getChildren().add(children);
    }

    public void addLinkIncoming(Link link) {
        getLinksIncoming().add(link);
    }

    public void addLinkOutgoing(Link link) {
        getLinksOutgoing().add(link);
    }

    public void addProperty(ElementProperty property) {
        getProperties().add(property);
    }

    /**
     * Returns a unmodifiable map with all properties of this element. Key of the
     * map is the key of the property.
     *
     * @return A unmodifiable map with all properties
     */
    @JsonIgnore
    public Map<String, List<ElementProperty>> getPropertyMap() {
        Map<String, List<ElementProperty>> propertyMap = new HashMap<>(getProperties().size());
        for (ElementProperty property : getProperties()) {
            List<ElementProperty> propertyList = propertyMap.get(property.getKey());
            if (propertyList == null) {
                // The cardinality of most properties is single
                propertyList = new ArrayList<>(1);
                propertyMap.put(property.getKey(), propertyList);
            }
            propertyList.add(property);
        }
        return Collections.unmodifiableMap(propertyMap);
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
        Element other = (Element) obj;
        if (uuid == null) {
            if (other.uuid != null) {
                return false;
            }
        } else if (!uuid.equals(other.uuid)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return getTypeId() + " - " + getTitle() + " - " + getUuid();
    }

}
