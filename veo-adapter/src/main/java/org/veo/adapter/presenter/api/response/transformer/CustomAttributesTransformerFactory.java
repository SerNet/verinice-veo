/*******************************************************************************
 * Copyright (c) 2020 Jonas Jordan.
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
package org.veo.adapter.presenter.api.response.transformer;

import java.util.Collections;

import com.fasterxml.jackson.core.JsonProcessingException;

import org.veo.core.entity.EntitySchemaException;
import org.veo.core.service.EntitySchemaService;

import io.swagger.v3.core.util.Json;

/** Factory that creates @{code CustomAttributesTransformer} instances. */
class CustomAttributesTransformerFactory {
    private final EntitySchemaService entitySchemaService;

    public CustomAttributesTransformerFactory(EntitySchemaService entitySchemaService) {
        this.entitySchemaService = entitySchemaService;
    }

    public CustomAttributesTransformer createCustomAttributesTransformer(String entityType) {
        var schemaString = entitySchemaService.findSchema(entityType, Collections.emptyList());
        try {
            return new CustomAttributesTransformer(Json.mapper()
                                                       .readTree(schemaString),
                    new AttributeTransformer());
        } catch (JsonProcessingException ex) {
            throw new EntitySchemaException(
                    String.format("Invalid entity schema \"%s/", entityType), ex);
        }
    }
}
