/*******************************************************************************
 * Copyright (c) 2019 Urs Zeidler.
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
package org.veo.persistence.entity.jpa;

import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import org.veo.persistence.entity.jpa.custom.PropertyData;

@Entity(name = "customproperties")
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
public class CustomPropertiesData extends BaseModelObjectData {

    @Column(name = "type")
    @ToString.Include
    private String type;

    @ElementCollection(fetch = FetchType.EAGER)
    @Column(name = "applicableto")
    private Set<String> applicableTo;

    // many to one customproperties-> domain
    @Column(name = "domains")
    @ManyToMany
    private Set<DomainData> domains;

    public String getType() {
        return this.type;
    }

    public void setType(String aType) {
        this.type = aType;
    }

    public Set<String> getApplicableTo() {
        return this.applicableTo;
    }

    public void setApplicableTo(Set<String> aApplicableTo) {
        this.applicableTo = aApplicableTo;
    }

    public Set<DomainData> getDomains() {
        return this.domains;
    }

    public void setDomains(Set<DomainData> aDomains) {
        this.domains = aDomains;
    }

    @OneToMany(targetEntity = PropertyData.class,
               fetch = FetchType.EAGER,
               cascade = CascadeType.ALL,
               orphanRemoval = true,
               mappedBy = "parentId")
    private java.util.Set<PropertyData> dataProperties = new java.util.HashSet<>();

    public Set<PropertyData> getDataProperties() {
        return dataProperties;
    }

    public void setDataProperties(Set<PropertyData> dataProperties) {
        this.dataProperties = dataProperties;
        for (var dataProperty : dataProperties) {
            dataProperty.setParentId(getId());
        }
    }

}
