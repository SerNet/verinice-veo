package org.veo.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.veo.model.Element;
import org.veo.model.ElementProperty;
import org.veo.model.Property;
import org.veo.model.VeoException;
import org.veo.persistence.ElementRepository;

/**
 * ElementFactory creates or updates an element from a property map containing JSON data.
 */
@Service
public class ElementFactory {

    @Autowired
    ElementRepository elementRepository;

    public Element createElement(Map<String, Object> json) {
        String id = (String) json.get(JsonFactory.ID);
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        return updateElement(json, new Element(id));
    }

    public Element updateElement(Map<String, Object> json, Element element) {
        setStaticProperties(json, element);
        setDynamicProperties(json, element);
        return element;
    }

    private void setStaticProperties(Map<String, Object> json, Element element) {
        element.setTypeId((String) json.get(JsonFactory.TYPE));
        element.setTitle((String) json.get(JsonFactory.TITLE));
        setParent(json, element);
    }

    private void setParent(Map<String, Object> json, Element element) {
        String parentUuid = (String) json.get(JsonFactory.PARENT);
        if (parentUuid != null) {
            Element parent = elementRepository.findByUuid(parentUuid);
            if(parent==null) {
                throw new VeoException(VeoException.Error.ELEMENT_NOT_FOUND, String.format(VeoException.ELEMENT_NOT_EXISTS, parentUuid));
            }
            element.setParent(elementRepository.findByUuid(parentUuid));
        }
    }

    private void setDynamicProperties(Map<String, Object> json, Element element) {
        Map<String, List<ElementProperty>> propertyMap = element.getPropertyMap();
        for (Map.Entry<String, Object> entry : json.entrySet()) {
            if (!isStaticProperty(entry.getKey())) {
                List<ElementProperty> propertyList = setDynamicProperty(entry, propertyMap);
                addProperties(propertyList, element);
            }
        }
    }

    private List<ElementProperty> setDynamicProperty(Map.Entry<String, Object> entry, Map<String, List<ElementProperty>> propertyMap) {
        List<ElementProperty> propertyList = propertyMap.get(entry.getKey());
        if (propertyList == null) {
            propertyList = new ArrayList<>(1);
        }
        setDynamicProperty(entry, propertyList);
        return propertyList;
    }

    private void setDynamicProperty(Map.Entry<String, Object> entry, List<ElementProperty> properties) {
        Object value = entry.getValue();
        if (value instanceof Object[]) {
            setMultiProperties(entry.getKey(), (Object[]) value, properties);
        } else if (value instanceof List) {
            setMultiProperties(entry.getKey(), ((List) value).toArray(), properties);
        } else {
            setSingleProperty(entry.getKey(), value, properties);
        }
    }

    private void setMultiProperties(String key, Object[] valueList, List<ElementProperty> properties) {
        properties.clear();
        int index = 0;
        for (Object singleValue : valueList) {
            ElementProperty property = new ElementProperty();
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

    private void setSingleProperty(String key, Object value, List<ElementProperty> properties) {
        ElementProperty property;
        if (properties.isEmpty()) {
            property = new ElementProperty();
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

    private void addProperties(List<ElementProperty> propertyList, Element element) {
        for (ElementProperty property : propertyList) {
            if (!element.getProperties().contains(property)) {
                element.addProperty(property);
            }
        }
    }

    private boolean isStaticProperty(String name) {
        return JsonFactory.STATIC_PROPERTIES.contains(name);
    }
}