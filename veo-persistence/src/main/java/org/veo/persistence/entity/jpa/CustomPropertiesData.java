/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2019  Urs Zeidler.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.veo.persistence.entity.jpa;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.veo.core.entity.CustomProperties;
import org.veo.core.entity.Domain;
import org.veo.core.entity.EntityLayerSupertype;
import org.veo.persistence.entity.jpa.custom.PropertyData;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity(name = "customproperties")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@Data
public class CustomPropertiesData implements CustomProperties {

    @Id
    @ToString.Include
    private String dbId = UUID.randomUUID()
                              .toString();

    @ToString.Include
    @EqualsAndHashCode.Include
    private String type;

    @EqualsAndHashCode.Include
    @ManyToOne(fetch = FetchType.LAZY,
               targetEntity = EntityLayerSupertypeData.class,
               // 'links' are also customProperties, saved in the same table but mapped by
               // 'source'
               // column, due to the single-table inheritance mapping used here.
               // 'owner' must therefore be nullable for these entities:
               optional = true)
    private EntityLayerSupertype owner;

    @ElementCollection(fetch = FetchType.EAGER)
    @Column(name = "applicableto")
    private Set<String> applicableTo = new HashSet<>();

    @ManyToMany(targetEntity = DomainData.class)
    final protected Set<Domain> domains = new HashSet<>();

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

    public void setDataProperties(Set<PropertyData> newDataProperties) {
        this.dataProperties.clear();
        newDataProperties.forEach(propertyData -> propertyData.setParentId(this.dbId));
        this.dataProperties.addAll(newDataProperties);
    }

    @Override
    public Map<String, Double> getDoubleProperties() {
        return getProperties(Double.class);
    }

    @Override
    public void setProperty(String key, Double value) {
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
                             .filter(p -> inClass.isInstance(p.getValue()))
                             .collect(Collectors.toMap(PropertyData::getKey,
                                                       p -> cast.apply(p.getValue())));
    }

    /**
     * Add the given Domain to the collection domains.
     *
     * @return true if added
     */
    public boolean addToDomains(Domain aDomain) {
        return this.domains.add(aDomain);
    }

    /**
     * Remove the given Domain from the collection domains.
     *
     * @return true if removed
     */
    public boolean removeFromDomains(Domain aDomain) {
        return this.domains.remove(aDomain);
    }

    @Override
    public void setDomains(Set<Domain> newDomains) {
        domains.clear();
        domains.addAll(newDomains);
    }
}
