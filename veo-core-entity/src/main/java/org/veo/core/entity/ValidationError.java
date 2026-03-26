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
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.veo.core.entity.exception.UnprocessableDataException;
import org.veo.core.entity.inspection.Finding;
import org.veo.core.entity.inspection.Severity;

import edu.umd.cs.findbugs.annotations.NonNull;

public interface ValidationError {
  String message();

  Finding toDomainUpdateFinding(Domain domain);

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
  record Generic(String message) implements ValidationError {
    @Override
    public Finding toDomainUpdateFinding(Domain domain) {
      return new Finding(
          Severity.WARNING,
          TranslatedText.of(
              Map.of(
                  "en",
                  "The object cannot be migrated to the new domain version %s: %s"
                      .formatted(domain.getTemplateVersion(), message))),
          Collections.emptyList());
    }
  }

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

    @Override
    public Finding toDomainUpdateFinding(Domain domain) {
      return new Finding(Severity.WARNING, getDescription(domain), Collections.emptyList());
    }

    @NonNull
    private TranslatedText getDescription(Domain domain) {
      return TranslatedText.of(
          Map.of(
              "en",
              "The object cannot be migrated to the new domain version %s. In the new version, some attributes are shared with other domains (%s), but the object has deviating values in those domains:%n%n%s%n%nPlease edit this object here or in the other domains to align the deviating values."
                  .formatted(
                      domain.getTemplateVersion(),
                      formatConflictingDomains(Locale.ENGLISH),
                      formatConflictingAttributes(domain, Locale.ENGLISH)),
              "de",
              "Das Objekt ist nicht migrierbar auf die neue Domänen-Version %s. In der neuen Version werden einige Attribute gemeinsam genutzt mit anderen Domänen (%s). Dieses Objekt hat jedoch dort abweichende Werte:%n%n%s%n%nBitte bearbeiten Sie das Objekt hier oder in den anderen Domänen, um die abweichenden Werte aneinander anzugleichen."
                  .formatted(
                      domain.getTemplateVersion(),
                      formatConflictingDomains(Locale.GERMAN),
                      formatConflictingAttributes(domain, Locale.GERMAN))));
    }

    private String formatConflictingDomains(Locale locale) {
      return String.join(
          ", ", conflictingDomains.stream().map(d -> d.getTranslations(locale).getName()).toList());
    }

    private String formatConflictingAttributes(Domain domain, Locale locale) {
      // All conflicting domains should have the same values, so any of them will do for the
      // comparison.
      var otherDomain = conflictingDomains.getFirst();
      var ourAttributes =
          element
              .findCustomAspect(domain, caType)
              .map(CustomAspect::getAttributes)
              .orElse(Collections.emptyMap());
      var otherAttributes =
          element
              .findCustomAspect(otherDomain, caType)
              .map(CustomAspect::getAttributes)
              .orElse(Collections.emptyMap());
      var etd = otherDomain.getElementTypeDefinition(element.getType());
      return Stream.concat(ourAttributes.keySet().stream(), otherAttributes.keySet().stream())
          .distinct()
          .filter(
              attrKey -> !Objects.equals(ourAttributes.get(attrKey), otherAttributes.get(attrKey)))
          .map(
              attrKey ->
                  "* %s: %s"
                      .formatted(
                          otherDomain
                              .getElementTypeDefinition(element.getType())
                              .findTranslation(locale, attrKey),
                          etd.localizeCustomAspectAttributeValue(
                              caType, attrKey, otherAttributes.get(attrKey), locale)))
          .collect(Collectors.joining("\n"));
    }
  }
}
