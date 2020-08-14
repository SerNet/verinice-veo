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

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;

import org.veo.core.entity.CustomProperties;
import org.veo.core.entity.Domain;
import org.veo.core.entity.ModelObject;
import org.veo.core.entity.ModelPackage;
import org.veo.persistence.entity.jpa.custom.PropertyData;

import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity(name = "customproperties")
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
public class CustomPropertiesData extends BaseModelObjectData implements CustomProperties {

    @Column(name = "type")
    @ToString.Include
    private String type;
    @ElementCollection(fetch = FetchType.EAGER)
    @Column(name = "applicableto")
    private Set<String> applicableTo;
    // many to one customproperties-> domain
    @Column(name = "domains")
    @ManyToMany(targetEntity = DomainData.class)
    protected Set<Domain> domains;

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

    public Set<Domain> getDomains() {
        return this.domains;
    }

    public void setDomains(Set<Domain> aDomains) {
        this.domains = aDomains;
    }

    @OneToMany(targetEntity = PropertyData.class,
               fetch = FetchType.EAGER,
               cascade = CascadeType.ALL,
               orphanRemoval = true,
               mappedBy = "parentId")
    private java.util.Set<PropertyData> dataProperties = new java.util.HashSet<>();

    @Override
    public void clearProperties() {
        dataProperties.clear();
    }

    @Override
    public Map<String, Boolean> getBooleanProperties() {
        return getProperties(Boolean.class);
    }

    @Override
    public void setProperty(String key, Boolean value) {
        PropertyData propertyData = new PropertyData(key, value);
        propertyData.setParentId(getDbId());
        dataProperties.add(propertyData);
    }

    @Override
    public Map<String, Integer> getIntegerProperties() {
        return getProperties(Integer.class);
    }

    @Override
    public void setProperty(String key, Integer value) {
        PropertyData propertyData = new PropertyData(key, value);
        propertyData.setParentId(getDbId());
        dataProperties.add(propertyData);
    }

    @Override
    public Map<String, OffsetDateTime> getOffsetDateTimeProperties() {
        return getProperties(OffsetDateTime.class);
    }

    @Override
    public void setProperty(String key, OffsetDateTime value) {
        PropertyData propertyData = new PropertyData(key, value);
        propertyData.setParentId(getDbId());
        dataProperties.add(propertyData);
    }

    @Override
    public Map<String, String> getStringProperties() {
        return getProperties(String.class);
    }

    @Override
    public void setProperty(String key, String value) {
        PropertyData propertyData = new PropertyData(key, value);
        propertyData.setParentId(getDbId());
        dataProperties.add(propertyData);
    }

    @Override
    public Map<String, List<String>> getStringListProperties() {
        return getProperties(List.class, p -> (List<String>) p);
    }

    @Override
    public void setProperty(String key, List<String> value) {
        PropertyData propertyData = new PropertyData(key, value);
        propertyData.setParentId(getDbId());
        dataProperties.add(propertyData);
    }

    @Override
    public Map<String, ?> getAllProperties() {
        return new HashMap<>(getProperties(Object.class));
    }

    private <T> Map<String, T> getProperties(Class<T> tClass) {
        return getProperties(tClass, tClass::cast);
    }

    private <TOut> Map<String, TOut> getProperties(Class<?> inClass, Function<Object, TOut> cast) {
        return dataProperties.stream()
                             .filter(p -> inClass.isInstance(((PropertyData) p).getValue()))
                             .collect(Collectors.toMap(p -> ((PropertyData) p).getKey(),
                                                       p -> cast.apply(((PropertyData) p).getValue())));
    }

    /**
     * Add the given Domain to the collection domains.
     *
     * @return true if added
     */
    public boolean addToDomains(Domain aDomain) {
        boolean add = this.domains.add(aDomain);
        return add;
    }

    /**
     * Remove the given Domain from the collection domains.
     *
     * @return true if removed
     */
    public boolean removeFromDomains(Domain aDomain) {
        boolean remove = this.domains.remove(aDomain);
        return remove;
    }

    @Override
    public String getModelType() {
        return ModelPackage.ELEMENT_CUSTOMPROPERTIES;
    }

    @Override
    public Class<? extends ModelObject> getModelInterface() {
        return CustomProperties.class;
    }

}
