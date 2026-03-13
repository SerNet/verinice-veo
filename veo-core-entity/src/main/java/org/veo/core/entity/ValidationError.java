/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2026  Jonas Jordan
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.veo.core.entity.exception.UnprocessableDataException;

public interface ValidationError {
  String message();

  static void throwOnErrors(Collection<ValidationError> errors) {
    if (!errors.isEmpty()) {
      throw new UnprocessableDataException(
          errors.stream().map(ValidationError::message).collect(Collectors.joining(", ")));
    }
  }

  static List<ValidationError> mergeIfAny(
      String wrapperMessage, Collection<ValidationError> innerErrors) {
    if (innerErrors.isEmpty()) {
      return new ArrayList<>();
    }
    return List.of(
        new ValidationError.Generic(
            wrapperMessage
                + ": "
                + innerErrors.stream()
                    .map(ValidationError::message)
                    .collect(Collectors.joining(", "))));
  }

  // TODO replace this fallback type with specific errors with multilingual and more helpful
  // messages
  record Generic(String message) implements ValidationError {}

  record CustomAspectConflict(String caType, List<Domain> conflictingDomains, Element element)
      implements ValidationError {
    @Override
    public String message() {
      return "Custom aspect '%s' is shared with the domains %s, but element has conflicting values for the custom aspect in those domains."
          .formatted(
              caType,
              conflictingDomains.stream()
                  .map(d -> "%s %s".formatted(d.getName(), d.getTemplateVersion()))
                  .collect(Collectors.joining(", ")));
    }
  }
}
