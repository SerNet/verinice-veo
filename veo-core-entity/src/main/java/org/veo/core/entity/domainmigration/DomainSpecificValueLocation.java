/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2024  Urs Zeidler
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
package org.veo.core.entity.domainmigration;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import org.veo.core.entity.BreakingChange;
import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainBase;
import org.veo.core.entity.DomainTemplate;
import org.veo.core.entity.Element;
import org.veo.core.entity.ElementType;

import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes({
  @Type(value = CustomAspectAttribute.class, name = "customAspectAttribute"),
})
@Schema(
    description = "The location of a domain-specific value in an element",
    discriminatorProperty = "type",
    discriminatorMapping = {
      @DiscriminatorMapping(value = "customAspectAttribute", schema = CustomAspectAttribute.class)
    },
    oneOf = CustomAspectAttribute.class)
public interface DomainSpecificValueLocation {

  @Schema(description = "The element type")
  @NotNull
  ElementType elementType();

  void validate(DomainTemplate domain);

  boolean matches(BreakingChange breakingChange);

  @JsonIgnore
  String getLocationString();

  @JsonIgnore
  Class<?> getValueType(DomainBase domain);

  void applyValue(Element element, Domain domain, Object value);
}
