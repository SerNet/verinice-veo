/*******************************************************************************
 * Copyright (c) 2018 Urs Zeidler.
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
 *     Urs Zeidler uz[at]sernet.de - initial API and implementation
 ******************************************************************************/
package org.veo.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.veo.model.Element;
import org.veo.model.Link;
import org.veo.model.LinkProperty;
import org.veo.model.Property;
import org.veo.model.VeoException;
import org.veo.persistence.ElementRepository;
import org.veo.persistence.LinkRepository;

/**
 * A factory to creates link objects.
 * 
 */
@Service
public class LinkFactory extends AbstractVeoFactory {
    @Autowired
    LinkRepository linkRepository;
    @Autowired
    ElementRepository elementRepository;

    public Link createLink(Map<String, Object> json) {
        String id = (String) json.get(JsonFactory.ID);
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        return updateLink(json, new Link(id));
    }

    public Link updateLink(Map<String, Object> json, Link link) {
        setStaticProperties(json, link);
        setDynamicProperties(json, link);
        return link;
    }

    private void setStaticProperties(Map<String, Object> json, Link link) {
        link.setTypeId((String) json.get(JsonFactory.TYPE));
        link.setTitle((String) json.get(JsonFactory.TITLE));
        setSource(json, link);
        setTarget(json, link);
    }

    private void setSource(Map<String, Object> json, Link link) {
        String sourceUuid = (String) json.get(JsonFactory.SOURCE);
        if (sourceUuid != null) {
            Element parent = elementRepository.findByUuid(sourceUuid);
            if (parent == null) {
                throw new VeoException(VeoException.Error.ELEMENT_NOT_FOUND,
                        String.format(VeoException.ELEMENT_NOT_EXISTS, sourceUuid));
            }
            link.setSource(elementRepository.findByUuid(sourceUuid));
        }
    }

    private void setTarget(Map<String, Object> json, Link link) {
        String targetUuid = (String) json.get(JsonFactory.TARGET);
        if (targetUuid != null) {
            Element parent = elementRepository.findByUuid(targetUuid);
            if (parent == null) {
                throw new VeoException(VeoException.Error.ELEMENT_NOT_FOUND,
                        String.format(VeoException.ELEMENT_NOT_EXISTS, targetUuid));
            }
            link.setDestination(elementRepository.findByUuid(targetUuid));
        }
    }

    private void setDynamicProperties(Map<String, Object> json, Link link) {
        Map<String, List<LinkProperty>> propertyMap = link.getPropertyMap();
        for (Map.Entry<String, Object> entry : json.entrySet()) {
            if (!isStaticProperty(entry.getKey())) {
                List<LinkProperty> propertyList = setDynamicProperty(entry, propertyMap);
                addProperties(propertyList, link);
            }
        }
    }

    private List<LinkProperty> setDynamicProperty(Map.Entry<String, Object> entry,
            Map<String, List<LinkProperty>> propertyMap) {
        List<LinkProperty> propertyList = propertyMap.get(entry.getKey());
        if (propertyList == null) {
            propertyList = new ArrayList<>(1);
        }
        setDynamicProperty(entry, propertyList);
        return propertyList;
    }

    private void setDynamicProperty(Map.Entry<String, Object> entry,
            List<LinkProperty> properties) {
        Object value = entry.getValue();
        if (value instanceof Object[]) {
            setMultiProperties(entry.getKey(), (Object[]) value, properties);
        } else if (value instanceof List) {
            setMultiProperties(entry.getKey(), ((List<?>) value).toArray(), properties);
        } else {
            setSingleProperty(entry.getKey(), value, properties);
        }
    }

    private void setMultiProperties(String key, Object[] valueList, List<LinkProperty> properties) {
        properties.clear();
        int index = 0;
        for (Object singleValue : valueList) {
            LinkProperty property = new LinkProperty();
            property.setIndex(index);
            property.setCardinality(Property.Cardinality.MULTI);
            if (singleValue instanceof String) {
                property.setValue(key, (String) singleValue);
                property.setType(Property.Type.TEXT);
            }
            if (singleValue instanceof Integer) {
                property.setValue(key, String.valueOf(singleValue));
                property.setType(Property.Type.NUMBER);
            }
            properties.add(property);
            index++;
        }
    }

    private void setSingleProperty(String key, Object value, List<LinkProperty> properties) {
        LinkProperty property;
        if (properties.isEmpty()) {
            property = new LinkProperty();
            properties.add(property);
        } else {
            property = properties.get(0);
        }
        property.setCardinality(Property.Cardinality.SINGLE);
        if (value instanceof String) {
            property.setType(Property.Type.TEXT);
            property.setValue(key, (String) value);
        }
        if (value instanceof Integer) {
            property.setType(Property.Type.NUMBER);
            property.setValue(key, String.valueOf(value));
        }
    }

    private void addProperties(List<LinkProperty> propertyList, Link link) {
        for (LinkProperty property : propertyList) {
            if (!link.getProperties().contains(property)) {
                link.addProperty(property);
            }
        }
    }

}
