/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.veo.model;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;

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
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "source_uuid")
    private Element source;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "destination_uuid")
    private Element destination;
    
    @OneToMany(cascade = CascadeType.ALL,orphanRemoval = true,fetch = FetchType.LAZY)
    @OrderColumn
    @JoinColumn(name = "link_uuid")
    private List<LinkProperty> properties;
    
    public Link() {
        if (this.uuid == null) {
            UUID randomUUID = java.util.UUID.randomUUID();
            uuid = randomUUID.toString();
        }
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

    public List<LinkProperty> getProperties() {
        if(properties==null) {
            properties = new LinkedList<>();
        }
        return properties;
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

    public void setProperties(List<LinkProperty> properties) {
        this.properties = properties;
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
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Link other = (Link) obj;
		if (uuid == null) {
			if (other.uuid != null)
				return false;
		} else if (!uuid.equals(other.uuid))
			return false;
		return true;
	}
    
    
}
