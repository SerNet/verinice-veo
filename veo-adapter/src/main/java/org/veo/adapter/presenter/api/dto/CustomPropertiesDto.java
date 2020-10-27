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
package org.veo.adapter.presenter.api.dto;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.validation.Valid;
import javax.validation.ValidationException;
import javax.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.veo.adapter.presenter.api.Patterns;
import org.veo.adapter.presenter.api.common.ModelObjectReference;
import org.veo.adapter.presenter.api.openapi.ModelObjectReferenceCustomPropertiesDomains;
import org.veo.adapter.presenter.api.response.transformer.CustomAttributesTransformer;
import org.veo.adapter.presenter.api.response.transformer.DtoToEntityTransformer;
import org.veo.adapter.presenter.api.response.transformer.EntityToDtoTransformer;
import org.veo.core.entity.CustomProperties;
import org.veo.core.entity.Domain;
import org.veo.core.entity.ModelObject;
import org.veo.core.entity.transform.EntityFactory;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.ToString;

/**
 * Transfer object for CustomProperties.
 */
@Data
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
public class CustomPropertiesDto {

    @Schema(description = "A timestamp acc. to RFC 3339 specifying when this entity was created.",
            example = "1990-12-31T23:59:60Z")
    @Pattern(regexp = Patterns.DATETIME)
    private String createdAt;
    @Schema(description = "The username of the user who created this object.",
            example = "jane_doe",
            accessMode = Schema.AccessMode.READ_ONLY)
    private String createdBy;

    @Schema(description = "A timestamp acc. to RFC 3339 specifying when this version of the entity was saved.",
            example = "1990-12-31T23:59:60Z",
            accessMode = Schema.AccessMode.READ_ONLY)
    @Pattern(regexp = Patterns.DATETIME)
    private String updatedAt;

    @Schema(description = "The username of the user who last updated this object.",
            example = "jane_doe",
            accessMode = Schema.AccessMode.READ_ONLY)
    private String updatedBy;

    // TODO Add an example for the API documentation for field applicableTo.
    @Schema(description = "The applicableTo for the CustomProperties.",
            example = "<add example here>",
            required = false)
    private Set<String> applicableTo = Collections.emptySet();

    @ArraySchema(schema = @Schema(implementation = ModelObjectReferenceCustomPropertiesDomains.class))

    private Set<ModelObjectReference<Domain>> domains = Collections.emptySet();

    public static CustomPropertiesDto from(@Valid CustomProperties control) {
        return EntityToDtoTransformer.transformCustomProperties2Dto(control);
    }

    @JsonIgnore
    public Collection<ModelObjectReference<? extends ModelObject>> getReferences() {
        List<ModelObjectReference<? extends ModelObject>> list = new ArrayList<>();
        list.addAll(getDomains());
        return list;
    }

    // maybe this is the best way to handle this.
    // we can use the map here and the transform function creates the properties
    // in the entity so no need for dtoProperties

    @Schema(description = "The properties of the element described by the schema of the type attribute.",
            example = " name: 'value'",
            required = false)
    private Map<String, ?> attributes = new HashMap<>();

    public Map<String, ?> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, ?> attributes) {
        attributes.forEach((key, value) -> {
            // TODO: it doesn't seem right that we have to do this manually
            if (value instanceof String
                    && ((String) value).length() > CustomProperties.MAXIMUM_STRING_LENGTH) {
                throw new ValidationException(
                        "Property value for " + key + " exceeds maximum length of "
                                + NumberFormat.getInstance(Locale.US)
                                              .format(CustomProperties.MAXIMUM_STRING_LENGTH)
                                + " characters.");
            }
        });
        this.attributes = attributes;
    }

    public CustomProperties toEntity(EntityFactory factory, String type,
            CustomAttributesTransformer customAttributesTransformer) {
        return DtoToEntityTransformer.transformDto2CustomProperties(factory, this, type,
                                                                    customAttributesTransformer);
    }
}
