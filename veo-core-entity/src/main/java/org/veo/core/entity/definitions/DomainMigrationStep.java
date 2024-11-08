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

import java.util.Collections;
import java.util.List;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.veo.core.entity.Constraints;
import org.veo.core.entity.TranslatedText;

public record DomainMigrationStep(
    @NotNull @Size(max = Constraints.DEFAULT_STRING_MAX_LENGTH) String id,
    @NotNull TranslatedText description,
    @NotNull List<MigrationDefinition> oldDefinitions,
    List<MigrationDefinition> newDefinitions) {
  public DomainMigrationStep {
    if (newDefinitions == null) {
      newDefinitions = Collections.emptyList();
    }
  }
}
