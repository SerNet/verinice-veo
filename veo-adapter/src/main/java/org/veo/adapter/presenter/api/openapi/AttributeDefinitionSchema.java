/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Jonas Jordan
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
package org.veo.adapter.presenter.api.openapi;

import org.veo.core.entity.definitions.attribute.BooleanAttributeDefinition;
import org.veo.core.entity.definitions.attribute.DateAttributeDefinition;
import org.veo.core.entity.definitions.attribute.DateTimeAttributeDefinition;
import org.veo.core.entity.definitions.attribute.EnumAttributeDefinition;
import org.veo.core.entity.definitions.attribute.ExternalDocumentAttributeDefinition;
import org.veo.core.entity.definitions.attribute.IntegerAttributeDefinition;
import org.veo.core.entity.definitions.attribute.ListAttributeDefinition;
import org.veo.core.entity.definitions.attribute.TextAttributeDefinition;

import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    description = "Defines validation rules for an attribute in a custom aspect or link",
    discriminatorProperty = "type",
    discriminatorMapping = {
      @DiscriminatorMapping(
          schema = BooleanAttributeDefinition.class,
          value = BooleanAttributeDefinition.TYPE),
      @DiscriminatorMapping(
          schema = DateAttributeDefinition.class,
          value = DateAttributeDefinition.TYPE),
      @DiscriminatorMapping(
          schema = DateTimeAttributeDefinition.class,
          value = DateTimeAttributeDefinition.TYPE),
      @DiscriminatorMapping(
          schema = EnumAttributeDefinition.class,
          value = EnumAttributeDefinition.TYPE),
      @DiscriminatorMapping(
          schema = ExternalDocumentAttributeDefinition.class,
          value = ExternalDocumentAttributeDefinition.TYPE),
      @DiscriminatorMapping(
          schema = IntegerAttributeDefinition.class,
          value = IntegerAttributeDefinition.TYPE),
      @DiscriminatorMapping(
          schema = ListAttributeDefinition.class,
          value = ListAttributeDefinition.TYPE),
      @DiscriminatorMapping(
          schema = TextAttributeDefinition.class,
          value = TextAttributeDefinition.TYPE),
    })
public class AttributeDefinitionSchema {}
