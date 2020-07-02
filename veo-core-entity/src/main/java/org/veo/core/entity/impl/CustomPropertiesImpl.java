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
package org.veo.core.entity.impl;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import org.veo.core.entity.CustomProperties;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Key;
import org.veo.core.entity.ModelPackage;

/**
 * The base for all the custom aspects. This class should be extensible by
 * clients.
 */
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
public abstract class CustomPropertiesImpl extends BaseModelObject implements CustomProperties {

    @ToString.Include
    private String type;
    private Set<String> applicableTo = Collections.emptySet();
    private Set<Domain> domains = Collections.emptySet();

    public CustomPropertiesImpl(@NotNull Key<UUID> id) {
        super(id);
    }

    private final Map<String, Object> properties = new HashMap<>();

    @Override
    public void clearProperties() {
        properties.clear();
    }

    @Override
    public Map<String, Boolean> getBooleanProperties() {
        return getProperties(Boolean.class);
    }

    @Override
    public void setProperty(String key, Boolean value) {
        properties.put(key, value);
    }

    @Override
    public Map<String, Integer> getIntegerProperties() {
        return getProperties(Integer.class);
    }

    @Override
    public void setProperty(String key, Integer value) {
        properties.put(key, value);
    }

    @Override
    public Map<String, OffsetDateTime> getOffsetDateTimeProperties() {
        return getProperties(OffsetDateTime.class);
    }

    @Override
    public void setProperty(String key, OffsetDateTime value) {
        properties.put(key, value);
    }

    @Override
    public Map<String, String> getStringProperties() {
        return getProperties(String.class);
    }

    @Override
    public void setProperty(String key, String value) {
        properties.put(key, value);
    }

    @Override
    public Map<String, List<String>> getStringListProperties() {
        return getProperties(List.class, p -> (List<String>) p);
    }

    @Override
    public void setProperty(String key, List<String> value) {
        properties.put(key, value);
    }

    @Override
    public Map<String, ?> getAllProperties() {
        return new HashMap<>(properties);
    }

    private <T> Map<String, T> getProperties(Class<T> tClass) {
        return getProperties(tClass, tClass::cast);
    }

    private <TOut> Map<String, TOut> getProperties(Class<?> inClass, Function<Object, TOut> cast) {
        return properties.entrySet()
                         .stream()
                         .filter(o -> inClass.isInstance(o.getValue()))
                         .collect(Collectors.toMap(Map.Entry::getKey,
                                                   entry -> cast.apply(entry.getValue())));
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
    public String getType() {
        return this.type;
    }

    @Override
    public void setType(String aType) {
        this.type = aType;
    }

    @Override
    public Set<String> getApplicableTo() {
        return this.applicableTo;
    }

    @Override
    public void setApplicableTo(Set<String> aApplicableTo) {
        this.applicableTo = aApplicableTo;
    }

    @Override
    public Set<Domain> getDomains() {
        return this.domains;
    }

    @Override
    public void setDomains(Set<Domain> aDomains) {
        this.domains = aDomains;
    }

    @Override
    public String getModelType() {
        return ModelPackage.ELEMENT_CUSTOMPROPERTIES;
    }
}
