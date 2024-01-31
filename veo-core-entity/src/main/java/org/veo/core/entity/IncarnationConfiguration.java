/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2024  Jonas Jordan
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
package org.veo.core.entity;

import static java.util.Collections.unmodifiableSet;

import java.util.Set;

import jakarta.validation.constraints.NotNull;

import org.veo.core.entity.exception.UnprocessableDataException;

import lombok.NonNull;

/** Defines the behavior when incarnation descriptions are created for catalog items. */
public record IncarnationConfiguration(
    @NotNull @NonNull IncarnationRequestModeType mode,
    @NotNull @NonNull IncarnationLookup useExistingIncarnations,
    /**
     * An optional allow-list that determines which tailoring references are considered when
     * creating incarnation descriptions. Cannot be combined with {@link exclude}.
     */
    Set<TailoringReferenceType> include,
    /**
     * An optional deny-list that determines which tailoring references are ignored when creating
     * incarnation descriptions. Cannot be combined with {@link include}.
     */
    Set<TailoringReferenceType> exclude) {
  public IncarnationConfiguration {
    // validate
    if (include != null && exclude != null) {
      throw new UnprocessableDataException(
          "Cannot combine include and exclude lists (at least one of them must be null)");
    }

    // enforce immutability
    if (include != null) {
      include = unmodifiableSet(include);
    }
    if (exclude != null) {
      exclude = unmodifiableSet(exclude);
    }
  }

  public IncarnationConfiguration() {
    this(IncarnationRequestModeType.DEFAULT, IncarnationLookup.FOR_REFERENCED_ITEMS, null, null);
  }
}
