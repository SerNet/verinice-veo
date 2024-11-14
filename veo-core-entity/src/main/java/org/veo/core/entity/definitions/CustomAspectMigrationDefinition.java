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
package org.veo.core.entity.definitions;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import javax.annotation.Nullable;

import org.veo.core.entity.BreakingChange;
import org.veo.core.entity.Constraints;
import org.veo.core.entity.CustomAspect;
import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainBase;
import org.veo.core.entity.DomainTemplate;
import org.veo.core.entity.EntityType;
import org.veo.core.entity.condition.VeoExpression;

public record CustomAspectMigrationDefinition(
    @NotNull String type,
    @NotNull @Size(max = Constraints.DEFAULT_STRING_MAX_LENGTH) String elementType,
    @NotNull @Size(max = CustomAspect.TYPE_MAX_LENGTH) String customAspect,
    @NotNull @Size(max = Constraints.DEFAULT_STRING_MAX_LENGTH) String attribute,
    @Nullable VeoExpression migrationExpression)
    implements MigrationDefinition {

  @Override
  public void validate(DomainBase domain) {
    EntityType.validateElementType(elementType);
    ElementTypeDefinition elementTypeDefinition = domain.getElementTypeDefinition(elementType);
    var stepType = domain instanceof DomainTemplate ? "old" : "new";
    CustomAspectDefinition customAspectDefinition =
        elementTypeDefinition.getCustomAspects().get(customAspect);
    if (customAspectDefinition == null) {
      throw new IllegalArgumentException(
          "No customAspect '%s' for element type %s in %sDefinitions."
              .formatted(customAspect, elementType, stepType));
    }
    var attributeDefinition = customAspectDefinition.getAttributeDefinitions().get(attribute);
    if (attributeDefinition == null) {
      throw new IllegalArgumentException(
          "No attribute '%s.%s' for element type %s in %sDefinitions."
              .formatted(customAspect, attribute, elementType, stepType));
    }
    if (migrationExpression != null) {
      if (domain instanceof Domain d) {
        try {
          var domainTemplate = d.getDomainTemplate();
          migrationExpression.selfValidate(domainTemplate, elementType);
          Class<?> expectedType = attributeDefinition.getValueType();
          Class<?> actualType = migrationExpression.getValueType(domainTemplate, elementType);
          if (!actualType.isAssignableFrom(expectedType)) {
            throw new IllegalArgumentException(
                "Values for attribute %s must be of type %s, but given expression produces %s."
                    .formatted(
                        attribute, expectedType.getSimpleName(), actualType.getSimpleName()));
          }
        } catch (Exception e) {
          throw new IllegalArgumentException(
              "MigrationExpression is invalid: %s.".formatted(e.getMessage()));
        }
      } else {
        throw new IllegalArgumentException("Can't have expressions for old migration steps.");
      }
    }
  }

  @Override
  public boolean matches(BreakingChange breakingChange) {
    return breakingChange.type().equals("customAspectAttribute")
        && breakingChange.elementType().equals(elementType)
        && breakingChange.customAspect().equals(customAspect)
        && breakingChange.attribute().equals(attribute);
  }
}
