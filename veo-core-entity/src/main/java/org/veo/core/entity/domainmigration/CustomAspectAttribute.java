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
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import org.veo.core.entity.BreakingChange;
import org.veo.core.entity.Constraints;
import org.veo.core.entity.CustomAttributeContainer;
import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainBase;
import org.veo.core.entity.DomainTemplate;
import org.veo.core.entity.Element;
import org.veo.core.entity.ElementType;
import org.veo.core.entity.definitions.CustomAspectDefinition;
import org.veo.core.entity.definitions.ElementTypeDefinition;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A custom aspect attribute")
@JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
public record CustomAspectAttribute(
    @NotNull ElementType elementType,
    @NotNull
        @Schema(description = "The custom aspect")
        @Size(max = CustomAttributeContainer.TYPE_MAX_LENGTH)
        String customAspect,
    @NotNull
        @Size(max = Constraints.DEFAULT_STRING_MAX_LENGTH)
        @Schema(description = "The attribute in the custom aspect")
        String attribute)
    implements DomainSpecificValueLocation {

  @Override
  public Class<?> getValueType(DomainBase domain) {
    ElementTypeDefinition elementTypeDefinition = domain.getElementTypeDefinition(elementType());
    CustomAspectDefinition customAspectDefinition =
        elementTypeDefinition.getCustomAspects().get(customAspect());
    if (customAspectDefinition == null) {
      throw new IllegalArgumentException(
          "No customAspect '%s' for element type %s."
              .formatted(customAspect(), elementType().getSingularTerm()));
    }
    var attributeDefinition = customAspectDefinition.getAttributeDefinitions().get(attribute());
    if (attributeDefinition == null) {
      throw new IllegalArgumentException(
          "No attribute '%s.%s' for element type %s."
              .formatted(customAspect(), attribute(), elementType().getSingularTerm()));
    }
    return attributeDefinition.getValueType();
  }

  @Override
  public void validate(DomainTemplate domain) {
    getValueType(domain);
  }

  @Override
  public boolean matches(BreakingChange breakingChange) {
    return breakingChange.type().equals("customAspectAttribute")
        && breakingChange.elementType() == elementType()
        && breakingChange.customAspect().equals(customAspect())
        && breakingChange.attribute().equals(attribute());
  }

  @Override
  public String getLocationString() {
    return String.join(".", customAspect, attribute);
  }

  @Override
  public void applyValue(Element element, Domain domain, Object value) {
    element.findOrAddCustomAspect(domain, customAspect).getAttributes().put(attribute, value);
  }
}
