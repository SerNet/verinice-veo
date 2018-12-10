/*******************************************************************************
 * Copyright (c) 2018 Daniel Murygin.
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
 *     Daniel Murygin dm[at]sernet.de - initial API and implementation
 ******************************************************************************/
package org.veo.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.veo.model.Element;
import org.veo.model.ElementProperty;
import org.veo.model.Link;
import org.veo.model.LinkProperty;
import org.veo.model.Property;

/**
 * JsonFactory creates a map from the properties in an element. The property map
 * can easily be converted to JSON.
 */
@Service
public class JsonFactory {

    public static final String ID = "$veo.id";
    public static final String PARENT = "parent";
    public static final String TITLE = "$veo.title";
    public static final String TYPE = "$veo.type";
    public static final String SOURCE = "source";
    public static final String TARGET = "target";

    public static final Set<String> STATIC_PROPERTIES;
    static {
        STATIC_PROPERTIES = new HashSet<>(5);
        STATIC_PROPERTIES.add(ID);
        STATIC_PROPERTIES.add(PARENT);
        STATIC_PROPERTIES.add(TITLE);
        STATIC_PROPERTIES.add(TYPE);
    }

    public Map<String, Object> createJson(Element element) {
        Map<String, Object> json = new HashMap<>();
        json.putAll(createStaticProperties(element));
        json.putAll(createDynamicProperties(element));
        return json;
    }

    public Map<String, Object> createJson(Link link) {
        Map<String, Object> json = new HashMap<>();
        json.putAll(createStaticProperties(link));
        json.putAll(createDynamicProperties(link));
        return json;
    }

    private Map<String, String> createStaticProperties(Link link) {
        Map<String, String> staticProperties = new HashMap<>(5);
        staticProperties.put(ID, link.getUuid());
        staticProperties.put(SOURCE, link.getSource().getUuid());
        staticProperties.put(TARGET, link.getDestination().getUuid());
        if (link.getTitle() != null) {
            staticProperties.put(TITLE, link.getTitle());
        }
        staticProperties.put(TYPE, link.getTypeId());
        return staticProperties;
    }

    private Map<String, Object> createDynamicProperties(Link link) {
        Map<String, List<LinkProperty>> propertyMap = link.getPropertyMap();
        Map<String, Object> dynamicProperties = new HashMap<>(propertyMap.size());
        for (Map.Entry<String, List<LinkProperty>> entry : propertyMap.entrySet()) {
            dynamicProperties.put(entry.getKey(), getDynamicPropertyValue(entry.getValue()));
        }
        return dynamicProperties;
    }

    private Map<String, String> createStaticProperties(Element element) {
        Map<String, String> staticProperties = new HashMap<>(4);
        staticProperties.put(ID, element.getUuid());
        if (element.getParent() != null) {
            staticProperties.put(PARENT, element.getParent().getUuid());
        }
        staticProperties.put(TITLE, element.getTitle());
        staticProperties.put(TYPE, element.getTypeId());
        return staticProperties;
    }

    private Map<String, Object> createDynamicProperties(Element element) {
        Map<String, List<ElementProperty>> propertyMap = element.getPropertyMap();
        Map<String, Object> dynamicProperties = new HashMap<>(propertyMap.size());
        for (Map.Entry<String, List<ElementProperty>> entry : propertyMap.entrySet()) {
            dynamicProperties.put(entry.getKey(), getDynamicPropertyValue(entry.getValue()));
        }
        return dynamicProperties;
    }

    private Object getDynamicPropertyValue(List<? extends Property> values) {
        Object[] valueArray = new Object[values.size()];
        boolean isMulti = false;
        for (int i = 0; i < valueArray.length; i++) {
            Property property = values.get(i);
            valueArray[i] = property.parseValue();
            isMulti = property.isMulti();
        }
        return (valueArray.length == 1 && !isMulti) ? valueArray[0] : valueArray;
    }
}