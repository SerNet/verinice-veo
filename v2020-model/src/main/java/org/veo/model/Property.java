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
 *
 * Contributors:
 *     Daniel Murygin dm[at]sernet[dot]de - initial API and implementation
 ******************************************************************************/
package org.veo.model;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Lob;

/**
 *
 * @author Daniel Murygin <dm[at]sernet[dot]de>
 */
@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class Property implements Serializable {
    
    @Id
    @Column(length = 36)
    private String uuid;
    
    @Column(name="group_index", nullable = false)
    private int index = 0;
    
    @Column(nullable = false)
    private String typeId;
    
    @Column(length = 255)
    private String label;
    
    @Lob
    private String text;
    
    private Long number;
    
    private Date date;
    
    @Column(name="properties_order")
    private int propertiesOrder = 0;
    
    public Property() {
        if (this.uuid == null) {
            UUID randomUUID = java.util.UUID.randomUUID();
            uuid = randomUUID.toString();
        }
    }

    public String getUuid() {
        return uuid;
    }

    public int getIndex() {
        return index;
    }

    public String getTypeId() {
        return typeId;
    }

    public String getLabel() {
        return label;
    }

    public String getText() {
        return text;
    }

    public Long getNumber() {
        return number;
    }

    public Date getDate() {
        return date;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public void setTypeId(String id) {
        this.typeId = id;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setNumber(Long number) {
        this.number = number;
    }

    public void setDate(Date date) {
        this.date = date;
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
		Property other = (Property) obj;
		if (uuid == null) {
			if (other.uuid != null)
				return false;
		} else if (!uuid.equals(other.uuid))
			return false;
		return true;
	}
    
    @Override
    public String toString() {
    	StringBuilder sb = new StringBuilder(getTypeId());
    	sb.append(": ");
    	if(getDate()!=null) {
    		sb.append(getDate().toString());
    	}
    	if(getLabel()!=null) {
    		sb.append(getLabel());
    	}
    	if(getNumber()!=null) {
    		sb.append(getNumber());
    	}
    	if(getText()!=null) {
    		sb.append(getText());
    	}
    	return sb.toString();
    }
}
