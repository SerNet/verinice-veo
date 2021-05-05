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

import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import org.veo.core.entity.CustomLink;
import org.veo.core.entity.CustomProperties;

/**
 * Parses attributes of custom aspect / link DTOs according to an entity JSON
 * schema. This is the high-level transformer class; the actual low-level
 * parsing and applying is delegated to an {@code AttributeTransformer}.
 */
public class EntitySchema {
    public static final String KEY_ATTRIBUTES = "attributes";
    public static final String KEY_CUSTOM_ASPECTS = "customAspects";
    public static final String KEY_ITEMS = "items";
    public static final String KEY_LINKS = "links";
    public static final String KEY_PROPERTIES = "properties";
    public static final String KEY_TARGET = "target";
    public static final String KEY_TYPE = "type";
    public static final String KEY_ENUM = "enum";

    private final JsonNode jsonSchema;
    private final AttributeTransformer transformer;

    public EntitySchema(JsonNode jsonSchema, AttributeTransformer transformer) {
        this.jsonSchema = jsonSchema;
        this.transformer = transformer;
    }

    /**
     * Parses attributes in map and applies them to given target aspect. For custom
     * links, use {@code applyLinkAttributes} instead.
     *
     * @param input
     *            DTO attribute map (key: attribute type, value: attribute value).
     * @param target
     *            Custom aspect to apply the attribute map to.
     * @throws IllegalArgumentException
     *             If attributes don't conform to schema.
     */
    public void applyAspectAttributes(Map<String, ?> input, CustomProperties target) {
        apply(input, target, getAspectAttrPropsSchema(target.getType()));
    }

    /**
     * Parses attributes in map and applies them to given target link.
     *
     * @param input
     *            DTO attribute map (key: attribute type, value: attribute value).
     * @param target
     *            Custom link to apply the attribute map to.
     * @throws IllegalArgumentException
     *             If attributes don't conform to schema.
     */
    public void applyLinkAttributes(Map<String, ?> input, CustomLink target) {
        apply(input, target, getLinkAttrPropsSchema(target.getType()));
    }

    /**
     * Validates if the links target type is allowed by the schema.
     */
    public void validateLinkTarget(CustomLink link) throws IllegalArgumentException {
        final var validTargetTypes = jsonSchema.get(KEY_PROPERTIES)
                                               .get(KEY_LINKS)
                                               .get(KEY_PROPERTIES)
                                               .get(link.getType())
                                               .get(KEY_ITEMS)
                                               .get(KEY_PROPERTIES)
                                               .get(KEY_TARGET)
                                               .get(KEY_PROPERTIES)
                                               .get(KEY_TYPE)
                                               .get(KEY_ENUM);
        for (var type : validTargetTypes) {
            if (link.getTarget()
                    .getModelType()
                    .equalsIgnoreCase(type.asText())) {
                return;
            }
        }
        throw new IllegalArgumentException(
                String.format("link target of type '%s' hat to be one of %s", link.getTarget()
                                                                                  .getModelType(),
                              validTargetTypes));
    }

    private void apply(Map<String, ?> input, CustomProperties target, JsonNode attrPropSchema) {
        input.forEach((key, value) -> {
            JsonNode propSchema = attrPropSchema.get(key);
            if (propSchema == null) {
                throw new IllegalArgumentException(
                        String.format("Attribute type \"%s\" does not exist in schema.", key));
            }
            transformer.applyToEntity(key, value, propSchema, target);
        });
    }

    private JsonNode getAspectAttrPropsSchema(String aspectType) {
        var aspectSchema = jsonSchema.get(KEY_PROPERTIES)
                                     .get(KEY_CUSTOM_ASPECTS)
                                     .get(KEY_PROPERTIES)
                                     .get(aspectType);
        if (aspectSchema == null) {
            throw new IllegalArgumentException(
                    String.format("Custom aspect type \"%s\" does not exist in schema.",
                                  aspectType));
        }
        return aspectSchema.get(KEY_PROPERTIES)
                           .get(KEY_ATTRIBUTES)
                           .get(KEY_PROPERTIES);
    }

    private JsonNode getLinkAttrPropsSchema(String type) {
        var linkSchema = jsonSchema.get(KEY_PROPERTIES)
                                   .get(KEY_LINKS)
                                   .get(KEY_PROPERTIES)
                                   .get(type);
        if (linkSchema == null) {
            throw new IllegalArgumentException(
                    "Custom link type \"" + type + "\" does not exist in schema.");
        }
        return linkSchema.get(KEY_ITEMS)
                         .get(KEY_PROPERTIES)
                         .get(KEY_ATTRIBUTES)
                         .get(KEY_PROPERTIES);
    }
}
