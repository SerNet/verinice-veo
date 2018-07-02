package org.veo.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.veo.model.Element;
import org.veo.model.ElementProperty;

/**
 * JsonFactory creates a map from the properties in an element.
 * The property map can easily be converted to JSON.
 */
@Service
public class JsonFactory {

    public static final String ID = "id";
    public static final String SCHEMA = "schema";
    public static final String TITLE = "title";
    public static final String PARENT = "parent";
    public static final Set<String> STATIC_PROPERTIES;

    static {
        STATIC_PROPERTIES = new HashSet<>(4);
        STATIC_PROPERTIES.add(ID);
        STATIC_PROPERTIES.add(SCHEMA);
        STATIC_PROPERTIES.add(TITLE);
        STATIC_PROPERTIES.add(PARENT);
    }

    public static final String SCHEMA_FOLDER = "/schemas/";
    public static final String SCHEMA_FILE_SUFFIX = ".json";
    public static final String SCHEMA_FILE_TEMPLATE = SCHEMA_FOLDER + "%s" + SCHEMA_FILE_SUFFIX;

    public Map<String, Object> createJson(Element element) {
        Map<String, Object> json = new HashMap<>();
        json.putAll(createStaticProperties(element));
        json.putAll(createDynamicProperties(element));
        return json;
    }

    private Map<String, String> createStaticProperties(Element element) {
        Map<String, String> staticProperties = new HashMap<>(4);
        staticProperties.put(ID, element.getUuid());
        staticProperties.put(SCHEMA, createSchemaPath(element));
        staticProperties.put(TITLE, element.getTitle());
        if(element.getParent()!=null) {
            staticProperties.put(PARENT, element.getParent().getUuid());
        }
        return staticProperties;
    }

    private Map<String, Object> createDynamicProperties(Element element) {
        Map<String,List<ElementProperty>> propertyMap = element.getPropertyMap();
        Map<String, Object> dynamicProperties = new HashMap<>(propertyMap.size());
        for (Map.Entry<String,List<ElementProperty>> entry: propertyMap.entrySet()) {
            dynamicProperties.put(entry.getKey(), getDynamicPropertyValue(entry));
        }
        return dynamicProperties;
    }

    private Object getDynamicPropertyValue(Map.Entry<String, List<ElementProperty>> propertyEntry) {
        List<ElementProperty> values = propertyEntry.getValue();
        Object[] valueArray = new Object[values.size()];
        boolean isMulti = false;
        for(int i=0;i<valueArray.length;i++) {
            ElementProperty property = values.get(i);
            valueArray[i] = property.parseValue();
            isMulti = property.isMulti();
        }
        return (valueArray.length==1 && !isMulti) ? valueArray[0] : valueArray;
    }

    private String createSchemaPath(Element element) {
        return String.format(SCHEMA_FILE_TEMPLATE, element.getTypeId());
    }

}