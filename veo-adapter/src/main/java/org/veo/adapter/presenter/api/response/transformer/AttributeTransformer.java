/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Jonas Jordan.
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
package org.veo.adapter.presenter.api.response.transformer;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.databind.JsonNode;

import org.veo.core.entity.CustomProperties;

/** Parses individual DTO custom attributes using JSON schemas. */
class AttributeTransformer {

    private static final String FORMAT_DATE_TIME = "date-time";
    private static final String KEY_ENUM = "enum";
    private static final String KEY_FORMAT = "format";
    private static final String KEY_ITEMS = "items";
    private static final String KEY_TYPE = "type";
    private static final String TYPE_ARRAY = "array";
    private static final String TYPE_BOOLEAN = "boolean";
    private static final String TYPE_NUMBER = "number";
    private static final String TYPE_STRING = "string";

    /**
     * Parses attribute value using given JSON schema and applies it to the target
     * entity.
     *
     * @param key
     *            Attribute key
     * @param value
     *            Attribute value
     * @param attrSchema
     *            JSON schema for the attribute.
     * @param target
     *            Custom aspect / link to apply the parsed value to.
     * @throws IllegalArgumentException
     *             If value does not conform to schema.
     */
    public void applyToEntity(String key, Object value, JsonNode attrSchema,
            CustomProperties target) {
        if (attrSchema.has(KEY_ENUM)) {
            var stringValue = (String) value;
            validateEnumValue(attrSchema, stringValue);
            target.setProperty(key, stringValue);
            return;
        }

        if (!attrSchema.has(KEY_TYPE)) {
            throw new IllegalArgumentException(
                    String.format("Schema for property %s has no type.", key));
        }
        var propType = attrSchema.get(KEY_TYPE)
                                 .asText();

        try {
            switch (propType) {
            case TYPE_STRING:
                if (attrSchema.has(KEY_FORMAT) && attrSchema.get(KEY_FORMAT)
                                                            .asText()
                                                            .equals(FORMAT_DATE_TIME)) {
                    target.setProperty(key, OffsetDateTime.parse((String) value));
                } else {
                    target.setProperty(key, (String) value);
                }
                break;
            case TYPE_BOOLEAN:
                target.setProperty(key, (Boolean) value);
                break;
            case TYPE_NUMBER:
                target.setProperty(key, ((Number) value).doubleValue());
                break;
            case TYPE_ARRAY:
                var itemSchema = attrSchema.get(KEY_ITEMS);
                for (var item : (List<?>) value) {
                    validateEnumValue(itemSchema, (String) item);
                }
                target.setProperty(key, (List<String>) value);
                break;
            default:
                throw new IllegalArgumentException(
                        String.format("Illegal custom property type: %s", propType));
            }
        } catch (ClassCastException ex) {
            throw new IllegalArgumentException(
                    String.format("Invalid data type %s for property type %s", value.getClass(),
                                  propType));
        }
    }

    private void validateEnumValue(JsonNode attrSchema, String stringValue) {
        var enumValues = StreamSupport.stream(attrSchema.get(KEY_ENUM)
                                                        .spliterator(),
                                              false);
        if (enumValues.noneMatch(node -> node.asText()
                                             .equals(stringValue))) {
            throw new IllegalArgumentException(
                    String.format("Illegal enum value: %s", stringValue));
        }
    }
}
