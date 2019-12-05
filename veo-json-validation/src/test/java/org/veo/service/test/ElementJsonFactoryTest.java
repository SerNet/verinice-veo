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
 ******************************************************************************/
package org.veo.service.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;

import org.veo.model.Element;
import org.veo.model.ElementProperty;
import org.veo.model.Property;
import org.veo.service.ElementFactory;
import org.veo.service.JsonFactory;

@RunWith(SpringRunner.class)
@DataJpaTest
public class ElementJsonFactoryTest {

    private static final Logger logger = LoggerFactory.getLogger(ElementJsonFactoryTest.class.getName());

    private static final String ROLE = "role";
    private static final String ROLE_A = "role-a";
    private static final String ROLE_B = "role-b";

    private static final String BUSINESS_VALUE = "business-value";
    private static final int BUSINESS_VALUE_1 = 1;
    private static final int BUSINESS_VALUE_2 = 2;

    private static final String ASSET_TYPE = "asset-type";
    private static final String ASSET_TYPE_PHYSICAL = "physical";
    private static final String ASSET_TYPE_SOFTWARE = "software";

    @Autowired
    JsonFactory jsonFactory;

    @Autowired
    ElementFactory elementFactory;

    @Test
    public void testCreateJson() throws ProcessingException, IOException {
        Element asset = createAsset();

        Map<String, Object> assetJsonMap = jsonFactory.createJson(asset);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNodeAsset = mapper.valueToTree(assetJsonMap);

        final JsonNode schemaJsonNode = mapper.readTree(this.getClass()
                                                            .getClassLoader()
                                                            .getResourceAsStream("schemas/asset.json"));
        final JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
        final JsonSchema schema = factory.getJsonSchema(schemaJsonNode);

        ProcessingReport valdationReport = schema.validate(jsonNodeAsset);
        logger.debug(valdationReport.toString());
        assertTrue("JSON document cannot be validated", valdationReport.isSuccess());

        checkProperty(jsonNodeAsset, "asset-type", "physical");
        checkProperty(jsonNodeAsset, ROLE, ROLE_A, ROLE_B);
        checkProperty(jsonNodeAsset, BUSINESS_VALUE, BUSINESS_VALUE_1, BUSINESS_VALUE_2);
    }

    @Test
    public void testCreateElement() {
        String uuid = UUID.randomUUID()
                          .toString();
        Map<String, Object> assetJsonMap = createAssetJsonMap(uuid);

        Element element = elementFactory.createElement(assetJsonMap);
        assertNotNull(element);
        assertEquals(uuid, element.getUuid());
        assertEquals(assetJsonMap.get(JsonFactory.TITLE), element.getTitle());
        assertEquals("asset", element.getTypeId());

        Set<ElementProperty> properties = element.getProperties();
        Set<ElementProperty> propertiesRole = getElementProperties(properties, ROLE);
        assertEquals("Number of role properties is not 2", 2, propertiesRole.size());
        assertTrue("Property " + ROLE_A + " not found",
                   isPropertyValue(propertiesRole, ROLE, ROLE_A));
        assertTrue("Property " + ROLE_B + " not found",
                   isPropertyValue(propertiesRole, ROLE, ROLE_B));
        assertTrue("Property index 1 not found", isIndexValue(propertiesRole, ROLE, 0));
        assertTrue("Property index 2 not found", isIndexValue(propertiesRole, ROLE, 1));

        Set<ElementProperty> propertiesBusinessValue = getElementProperties(properties,
                                                                            BUSINESS_VALUE);
        assertTrue("Property " + BUSINESS_VALUE_1 + " not found",
                   isPropertyValue(propertiesBusinessValue, BUSINESS_VALUE, BUSINESS_VALUE_1));
        assertTrue("Property " + BUSINESS_VALUE_2 + " not found",
                   isPropertyValue(propertiesBusinessValue, BUSINESS_VALUE, BUSINESS_VALUE_2));
        assertTrue("Property index 1 not found",
                   isIndexValue(propertiesBusinessValue, BUSINESS_VALUE, 0));
        assertTrue("Property index 2 not found",
                   isIndexValue(propertiesBusinessValue, BUSINESS_VALUE, 1));

        assertTrue("Property \"" + ASSET_TYPE_PHYSICAL + "\" not found",
                   isPropertyValue(properties, ASSET_TYPE, ASSET_TYPE_PHYSICAL));
    }

    @Test
    public void testUpdateElement() {
        String uuid = UUID.randomUUID()
                          .toString();
        Map<String, Object> assetJsonMap = createAssetJsonMap(uuid);
        Element element = elementFactory.createElement(assetJsonMap);
        Set<ElementProperty> propertyListAssetType = getElementProperties(element.getProperties(),
                                                                          ASSET_TYPE);
        assertEquals(1, propertyListAssetType.size());
        ElementProperty assetType = propertyListAssetType.iterator()
                                                         .next();

        assetJsonMap.put(ASSET_TYPE, ASSET_TYPE_SOFTWARE);

        Element updatedElement = elementFactory.updateElement(assetJsonMap, element);

        Set<ElementProperty> updatedElementProperties = updatedElement.getProperties();
        assertTrue("Property \"" + ASSET_TYPE_SOFTWARE + "\" not found",
                   isPropertyValue(updatedElementProperties, ASSET_TYPE, ASSET_TYPE_SOFTWARE));

        propertyListAssetType = getElementProperties(updatedElementProperties, ASSET_TYPE);
        assertEquals(1, propertyListAssetType.size());
        ElementProperty updatedAssetType = propertyListAssetType.iterator()
                                                                .next();

        assertEquals(assetType.getUuid(), updatedAssetType.getUuid());
    }

    private Element createAsset() {
        Element assetGroup = new Element();
        assetGroup.setTitle("Asset group");
        Element asset = new Element();
        asset.setTitle("Hello Asset!");
        asset.setParent(assetGroup);
        asset.setTypeId("asset");
        Set<ElementProperty> properties = new HashSet<>();

        ElementProperty propertyAssetType = new ElementProperty();
        propertyAssetType.setValue("asset-type", "physical");
        properties.add(propertyAssetType);

        ElementProperty propertyRoleA = new ElementProperty();
        propertyRoleA.setType(Property.Type.TEXT);
        propertyRoleA.setCardinality(Property.Cardinality.MULTI);
        propertyRoleA.setValue(ROLE, ROLE_A);
        propertyRoleA.setIndex(0);
        properties.add(propertyRoleA);

        ElementProperty propertyRoleB = new ElementProperty();
        propertyRoleB.setType(Property.Type.TEXT);
        propertyRoleB.setCardinality(Property.Cardinality.MULTI);
        propertyRoleB.setValue(ROLE, ROLE_B);
        propertyRoleB.setIndex(1);
        properties.add(propertyRoleB);

        ElementProperty businessValue1 = new ElementProperty();
        businessValue1.setType(Property.Type.NUMBER);
        businessValue1.setCardinality(Property.Cardinality.MULTI);
        businessValue1.setValue(BUSINESS_VALUE, String.valueOf(BUSINESS_VALUE_1));
        properties.add(businessValue1);

        ElementProperty businessValue2 = new ElementProperty();
        businessValue2.setType(Property.Type.NUMBER);
        businessValue2.setCardinality(Property.Cardinality.MULTI);
        businessValue2.setValue(BUSINESS_VALUE, String.valueOf(BUSINESS_VALUE_2));
        properties.add(businessValue2);

        asset.setProperties(properties);
        return asset;
    }

    private Map<String, Object> createAssetJsonMap(String uuid) {
        Map<String, Object> assetJsonMap = new HashMap<>(10);
        assetJsonMap.put(JsonFactory.ID, uuid);
        assetJsonMap.put(JsonFactory.TITLE, "Asset");
        assetJsonMap.put(JsonFactory.TYPE, "asset");
        assetJsonMap.put(ASSET_TYPE, ASSET_TYPE_PHYSICAL);
        assetJsonMap.put(ROLE, new String[] { ROLE_A, ROLE_B });
        assetJsonMap.put(BUSINESS_VALUE, new Integer[] { 1, 2 });
        return assetJsonMap;
    }

    private Set<ElementProperty> getElementProperties(Set<ElementProperty> allProperties,
            String key) {
        Set<ElementProperty> properties = new HashSet<>(2);
        for (ElementProperty property : allProperties) {
            if (key.equals(property.getKey())) {
                properties.add(property);
            }
        }
        return properties;
    }

    private void checkProperty(JsonNode jsonNode, String key, Object... expectedValues) {
        JsonNode property = jsonNode.get(key);
        assertNotNull("Property with key: " + key + " does not exists.", property);
        List<Object> values = getValues(property);
        for (Object expected : expectedValues) {
            assertTrue("Could not find value(s) " + expected + " of property " + key,
                       values.contains(expected));
        }

    }

    private List<Object> getValues(JsonNode property) {
        List<Object> values = new LinkedList<>();
        Iterator<JsonNode> valueIterator = property.elements();
        if (valueIterator.hasNext()) {
            while (valueIterator.hasNext()) {
                JsonNode node = valueIterator.next();
                if (node.canConvertToInt()) {
                    values.add(node.asInt());
                } else {
                    values.add(node.asText());
                }
            }
        } else {
            values.add(property.asText());
        }
        return values;
    }

    private boolean isPropertyValue(Set<ElementProperty> properties, String key,
            Object expectedValue) {
        for (ElementProperty p : properties) {
            if (key.equals(p.getKey()) && expectedValue.equals(p.parseValue())) {
                return true;
            }
        }
        return false;
    }

    private boolean isIndexValue(Set<ElementProperty> properties, String key, int index) {
        for (ElementProperty p : properties) {
            if (key.equals(p.getKey()) && index == p.getIndex()) {
                return true;
            }
        }
        return false;
    }
}