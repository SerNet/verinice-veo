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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

import org.veo.core.entity.condition.VeoExpression;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public record CustomAspectMigrationTransformDefinition(
    @NotNull VeoExpression migrationExpression, @JsonUnwrapped CustomAspectAttribute target)
    implements MigrationTransformDefinition {

  // work around https://github.com/FasterXML/jackson-databind/issues/3726
  @JsonCreator
  public CustomAspectMigrationTransformDefinition(
      @JsonProperty("elementType") String elementType,
      @JsonProperty("customAspect") String customAspect,
      @JsonProperty("attribute") String attribute,
      @JsonProperty(value = "migrationExpression", required = true)
          VeoExpression migrationExpression) {
    this(migrationExpression, new CustomAspectAttribute(elementType, customAspect, attribute));
  }
}
