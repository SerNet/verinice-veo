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
package org.veo.core.entity.definitions.attribute;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;
import static java.util.Collections.emptyList;

import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import org.veo.core.entity.exception.InvalidAttributeException;

import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@JsonTypeInfo(use = NAME, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(
      value = BooleanAttributeDefinition.class,
      name = BooleanAttributeDefinition.TYPE),
  @JsonSubTypes.Type(value = DateAttributeDefinition.class, name = DateAttributeDefinition.TYPE),
  @JsonSubTypes.Type(
      value = DateTimeAttributeDefinition.class,
      name = DateTimeAttributeDefinition.TYPE),
  @JsonSubTypes.Type(value = EnumAttributeDefinition.class, name = EnumAttributeDefinition.TYPE),
  @JsonSubTypes.Type(
      value = ExternalDocumentAttributeDefinition.class,
      name = ExternalDocumentAttributeDefinition.TYPE),
  @JsonSubTypes.Type(
      value = IntegerAttributeDefinition.class,
      name = IntegerAttributeDefinition.TYPE),
  @JsonSubTypes.Type(value = ListAttributeDefinition.class, name = ListAttributeDefinition.TYPE),
  @JsonSubTypes.Type(value = TextAttributeDefinition.class, name = TextAttributeDefinition.TYPE),
})
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
@Data
public abstract class AttributeDefinition {

  public abstract void validate(Object value) throws InvalidAttributeException;

  /**
   * Returns any translation keys that must be defined for this attribute. Does NOT include the
   * attribute's key since the {@link AttributeDefinition} object does not know its own key.
   */
  @JsonIgnore
  public Collection<String> getTranslationKeys() {
    return emptyList();
  }

  /**
   * @return expected type for values of this attribute
   */
  @JsonIgnore
  public abstract Class<?> getValueType();
}
