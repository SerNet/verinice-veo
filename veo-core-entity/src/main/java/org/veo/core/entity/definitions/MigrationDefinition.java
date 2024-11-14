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

import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import org.veo.core.entity.BreakingChange;
import org.veo.core.entity.DomainBase;
import org.veo.core.entity.condition.VeoExpression;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    property = "type",
    visible = true,
    include = JsonTypeInfo.As.EXISTING_PROPERTY)
@JsonSubTypes({
  @Type(value = CustomAspectMigrationDefinition.class, name = "customAspectAttribute"),
})
public interface MigrationDefinition {
  @NotNull
  String type();

  @NotNull
  String elementType();

  @Nullable
  VeoExpression migrationExpression();

  void validate(DomainBase domain);

  boolean matches(BreakingChange breakingChange);
}
