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

import org.veo.core.entity.Constraints;
import org.veo.core.entity.CustomAspect;
import org.veo.core.entity.DomainBase;
import org.veo.core.entity.EntityType;
import org.veo.core.entity.condition.VeoExpression;

public record CustomAspectMigrationDefinition(
    @NotNull String type,
    @NotNull @Size(max = Constraints.DEFAULT_STRING_MAX_LENGTH) String elementType,
    @NotNull @Size(max = CustomAspect.TYPE_MAX_LENGTH) String customAspect,
    @NotNull @Size(max = Constraints.DEFAULT_STRING_MAX_LENGTH) String attribute,
    boolean removeIfInvalid,
    @Nullable VeoExpression migrationExpression)
    implements MigrationDefinition {

  @Override
  public void validate(DomainBase domain) {
    EntityType.validateElementType(elementType);
    var attributeDefinition =
        domain
            .getElementTypeDefinition(elementType)
            .getCustomAspectDefinition(customAspect)
            .getAttributeDefinition(attribute);

    if (migrationExpression != null) {
      migrationExpression.selfValidate(domain, elementType);
      Class<?> expectedType = attributeDefinition.getValueType();
      Class<?> actualType = migrationExpression.getValueType(domain, elementType);
      if (!actualType.isAssignableFrom(expectedType)) {
        throw new IllegalArgumentException(
            "Values for attribute %s must be of type %s, but given expression produces %s"
                .formatted(attribute, expectedType.getSimpleName(), actualType.getSimpleName()));
      }
    }
  }
}
