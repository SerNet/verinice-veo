/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.veo.model;

import java.io.Serializable;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
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

/**
 *
 * @author Daniel Murygin
 */
@Entity
@NamedEntityGraphs({
    @NamedEntityGraph(
            name="linksWithProperties", 
            attributeNodes = {
            @NamedAttributeNode(value="linksOutgoing", subgraph = "linksGraph")
    },
    subgraphs = {
            @NamedSubgraph(
                    name = "linksGraph",
                    attributeNodes = {
                            @NamedAttributeNode(value = "properties")
                    }
            )        
    })
})
public class Element implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(length = 36)
    private String uuid;

    @Column(nullable = false, length = 255)
    private String title;
    
    @ManyToOne(cascade = {CascadeType.MERGE, CascadeType.REFRESH}, fetch = FetchType.EAGER)
    @JoinColumn(name = "scope_uuid")
    private Element scope;

    @ManyToOne(cascade = {CascadeType.MERGE, CascadeType.REFRESH}, fetch = FetchType.EAGER)
    @JoinColumn(name = "parent_uuid")
    private Element parent;
    
    @OneToMany(cascade = CascadeType.ALL)
    @OrderColumn
    @JoinColumn(name = "parent_uuid") 
    private Set<Element> children = new HashSet<>();
    
    @OneToMany(cascade = {CascadeType.MERGE, CascadeType.REFRESH})
    @OrderColumn
    @JoinColumn(name = "source_uuid") 
    private List<Link> linksOutgoing = new LinkedList<>();
    
    @OneToMany(cascade = {CascadeType.MERGE, CascadeType.REFRESH})
    @JoinColumn(name = "destination_uuid") 
    private List<Link> linksIncoming = new LinkedList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "element_uuid")
    private List<ElementProperty> properties;
    
    public Element() {
        if (this.uuid == null) {
            UUID randomUUID = java.util.UUID.randomUUID();
            uuid = randomUUID.toString();
        }
    }
    
    public Element(String uuid) {
       this.uuid = uuid;
    }
    
    public String getUuid() {
        return uuid;
    }

    public String getTitle() {
        return title;
    }

    public Element getParent() {
        return parent;
    }
    
    public Set<Element> getChildren() {
        return children;
    }
    
    public List<Link> getLinksOutgoing() {
        return linksOutgoing;
    }

    public List<Link> getLinksIncoming() {
        return linksIncoming;
    }

    public List<Element> getLinkedDestinations() {
        List<Element> linksOutgoing = new LinkedList<>();
        if(getLinksOutgoing()!=null) {
            for (Link link : getLinksOutgoing()) {
                linksOutgoing.add(link.getDestination());
            }
        }
        return linksOutgoing;
    }
    
    

    public Element getScope() {
        return scope;
    }

    public List<ElementProperty> getProperties() {
        if(properties==null) {
            properties = new LinkedList<>();
        }
        return properties;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setParent(Element parent) {
        this.parent = parent;
    }
    
    public void setChildren(Set<Element> children) {
        this.children = children;
    }
    
    public void addChild(Element children) {
        getChildren().add(children);
    }

    public void setLinksOutgoing(List<Link> linksOutgoing) {
        this.linksOutgoing = linksOutgoing;
    }
    
    public void addLinkIncoming(Link link) {
        getLinksIncoming().add(link);
    }

    public void setLinksIncoming(List<Link> linksIncoming) {
        this.linksIncoming = linksIncoming;
    }
    
    public void addLinkOutgoing(Link link) {
        getLinksOutgoing().add(link);
    }

    public void setScope(Element scope) {
        this.scope = scope;
    }

    public void setProperties(List<ElementProperty> properties) {
        this.properties = properties;
    }
    
    public void addProperty(ElementProperty property) {
        getProperties().add(property);
    }
    
}
