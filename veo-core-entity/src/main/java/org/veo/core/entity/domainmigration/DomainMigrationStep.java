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

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.veo.core.entity.BreakingChange;
import org.veo.core.entity.Constraints;
import org.veo.core.entity.TranslatedText;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    description =
        "One step in the migration of the domain. A step is a set of removal and/or transform operations described by a set of old and new definitions.")
public record DomainMigrationStep(
    @Schema(description = "The step id, needs to be unique in the list of migration steps")
        @NotNull
        @Size(max = Constraints.DEFAULT_STRING_MAX_LENGTH)
        String id,
    @Schema(
            description =
                "A translatable description of the migration step, which could be displayed in the manual migration. Key is ISO language key, value is human-readable description.")
        @NotNull
        TranslatedText description,
    @Schema(description = "A list of attributes in the old domain that this step handles") @NotNull
        List<DomainSpecificValueLocation> oldDefinitions,
    @Schema(
            description =
                "An optional list of attributes in the new domain that this step will create. If this is omitted, the values will not be transferred into the new domain.")
        List<MigrationTransformDefinition> newDefinitions) {
  public DomainMigrationStep {
    if (newDefinitions == null) {
      newDefinitions = Collections.emptyList();
    }
  }

  public boolean handles(BreakingChange breakingChange) {
    return oldDefinitions.stream().anyMatch(d -> d.matches(breakingChange));
  }
}
