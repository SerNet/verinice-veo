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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.validation.constraints.NotNull;

import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainTemplate;
import org.veo.core.entity.exception.UnprocessableDataException;

public record DomainMigrationDefinition(@NotNull List<DomainMigrationStep> migrations) {

  public void validate(@NotNull Domain domain, @NotNull DomainTemplate domainTemplate) {
    Set<String> ids = HashSet.newHashSet(migrations().size());
    migrations()
        .forEach(
            step -> {
              if (step.description().getTranslations().isEmpty()) {
                throw new UnprocessableDataException(
                    "No description provided for step '%s'.".formatted(step.id()));
              }
              if (ids.contains(step.id())) {
                throw new UnprocessableDataException("Id '%s' not unique.".formatted(step.id()));
              }
              ids.add(step.id());
              try {
                step.newDefinitions().forEach(nd -> nd.validate(domain));
                step.oldDefinitions().forEach(nd -> nd.validate(domainTemplate));
              } catch (IllegalArgumentException e) {
                throw new UnprocessableDataException(
                    "Invalid definition '%s'. %s".formatted(step.id(), e.getMessage()));
              }
            });
  }
}
