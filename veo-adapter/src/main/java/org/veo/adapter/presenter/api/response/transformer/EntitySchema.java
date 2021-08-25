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
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;

import org.veo.core.entity.CustomAspect;
import org.veo.core.entity.CustomLink;

import lombok.NonNull;

/**
 * Wraps a JSON entity schema and provides validation functionality for custom
 * aspects and links.
 */
public class EntitySchema {
    public static final String KEY_ATTRIBUTES = "attributes";
    public static final String KEY_CUSTOM_ASPECTS = "customAspects";
    public static final String KEY_ITEMS = "items";
    public static final String KEY_LINKS = "links";
    public static final String KEY_PROPERTIES = "properties";
    public static final String KEY_TARGET = "target";
    public static final String KEY_TYPE = "type";
    public static final String KEY_SUB_TYPE = "subType";
    public static final String KEY_ENUM = "enum";

    private final JsonNode jsonSchema;
    private final JsonSchemaFactory schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
    private final ObjectMapper om = new ObjectMapper();

    public EntitySchema(JsonNode jsonSchema) {
        this.jsonSchema = jsonSchema;
    }

    /**
     * Validates if the custom aspects attributes are allowed by the schema.
     */
    public void validateCustomAspect(CustomAspect customAspect) {
        validateAgainstSchema(customAspect.getAttributes(),
                              getAspectAttrSchema(customAspect.getType()));
    }

    /**
     * Validates if the link attributes and target type are allowed by the schema.
     */
    public void validateCustomLink(CustomLink customLink) {
        validateAgainstSchema(customLink.getAttributes(), getLinkAttrSchema(customLink.getType()));
        validateLinkTargetType(customLink);
        validateLinkTargetSubType(customLink);
    }

    private void validateAgainstSchema(@NonNull Map<String, Object> target,
            @NonNull JsonNode schema) {
        var errors = schemaFactory.getSchema(schema)
                                  .validate(om.convertValue(target, JsonNode.class));
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException("JSON does not conform to schema: " + errors.stream()
                                                                                           .map(ValidationMessage::getMessage)
                                                                                           .collect(Collectors.joining(", ")));
        }
    }

    private void validateLinkTargetType(CustomLink link) throws IllegalArgumentException {
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
                String.format("link target of type '%s' had to be one of %s", link.getTarget()
                                                                                  .getModelType(),
                              validTargetTypes));
    }

    private void validateLinkTargetSubType(CustomLink link) throws IllegalArgumentException {
        final var subTypeNode = jsonSchema.get(KEY_PROPERTIES)
                                          .get(KEY_LINKS)
                                          .get(KEY_PROPERTIES)
                                          .get(link.getType())
                                          .get(KEY_ITEMS)
                                          .get(KEY_PROPERTIES)
                                          .get(KEY_TARGET)
                                          .get(KEY_PROPERTIES)
                                          .get(KEY_SUB_TYPE);
        if (subTypeNode == null) {
            return;
        }
        var target = link.getTarget();
        var targetSubTypes = target.getDomains()
                                   .stream()
                                   .map(target::getSubType)
                                   .filter(Optional::isPresent)
                                   .map(Optional::get)
                                   .collect(Collectors.toSet());
        var allowedSubTypes = StreamSupport.stream(subTypeNode.get(KEY_ENUM)
                                                              .spliterator(),
                                                   false)
                                           .map(JsonNode::asText)
                                           .collect(Collectors.toSet());
        if (allowedSubTypes.stream()
                           .anyMatch(targetSubTypes::contains)) {
            return;
        }
        throw new IllegalArgumentException(
                String.format("link target with sub types %s had to have one of %s", targetSubTypes,
                              allowedSubTypes));
    }

    private JsonNode getAspectAttrSchema(String aspectType) {
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
                           .get(KEY_ATTRIBUTES);
    }

    private JsonNode getLinkAttrSchema(String type) {
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
                         .get(KEY_ATTRIBUTES);
    }
}
