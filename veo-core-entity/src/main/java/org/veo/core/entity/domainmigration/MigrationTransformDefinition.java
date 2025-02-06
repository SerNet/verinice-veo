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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import jakarta.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainTemplate;
import org.veo.core.entity.Element;
import org.veo.core.entity.ElementType;
import org.veo.core.entity.condition.VeoExpression;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes({
  @Type(value = CustomAspectMigrationTransformDefinition.class, name = "customAspectAttribute"),
})
@Schema(
    description =
        "Describes a transform operation to translate a value from the old domain to the new",
    discriminatorProperty = "type",
    discriminatorMapping =
        @DiscriminatorMapping(
            value = "customAspectAttribute",
            schema = CustomAspectMigrationTransformDefinition.class))
public interface MigrationTransformDefinition {

  @SuppressFBWarnings("SLF4J_LOGGER_SHOULD_BE_PRIVATE")
  Logger LOGGER = LoggerFactory.getLogger(MigrationTransformDefinition.class);

  @NotNull
  @Schema(description = "An expression to transform the value from the old domain")
  VeoExpression migrationExpression();

  DomainSpecificValueLocation target();

  default void migrate(
      Map<ElementType, List<Element>> elementsByType, Domain oldDomain, Domain newDomain) {
    elementsByType
        .getOrDefault(target().elementType(), Collections.emptyList())
        .forEach(
            element -> {
              var value = migrationExpression().getValue(element, oldDomain);
              LOGGER.debug(
                  "set value '{}' to {} by expression {}",
                  value,
                  target().getLocationString(),
                  migrationExpression());
              if (value != null) {
                target().applyValue(element, newDomain, value);
              }
            });
  }

  default void validate(Domain domain, DomainTemplate domainTemplate) {
    Class<?> expectedType = target().getValueType(domain);
    try {
      migrationExpression().selfValidate(domainTemplate, target().elementType());
      Class<?> actualType =
          migrationExpression().getValueType(domainTemplate, target().elementType());
      if (!actualType.isAssignableFrom(expectedType)) {
        throw new IllegalArgumentException(
            "Values for %s must be of type %s, but given expression produces %s."
                .formatted(
                    target().getLocationString(),
                    expectedType.getSimpleName(),
                    actualType.getSimpleName()));
      }
    } catch (Exception e) {
      throw new IllegalArgumentException(
          "MigrationExpression is invalid: %s.".formatted(e.getMessage()));
    }
  }
}
