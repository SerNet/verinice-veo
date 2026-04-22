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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.LoggerFactory;

import org.veo.core.entity.exception.UnprocessableDataException;

public interface ValidationError {
  String getMessage(Locale locale);

  static ValidationError localized(String messageKey, Object... messageArgs) {
    return localized(
        messageKey,
        Arrays.stream(messageArgs)
            .map(s -> (Function<Locale, Object>) (_) -> s == null ? "-" : s.toString())
            .toList());
  }

  static ValidationError localized(String messageKey, List<Function<Locale, Object>> messageArgs) {
    return locale -> {
      ResourceBundle messages = ResourceBundle.getBundle("validation", locale);
      try {
        var template = messages.getString(messageKey);
        return template.formatted(messageArgs.stream().map(a -> a.apply(locale)).toArray());
      } catch (Exception ex) {
        LoggerFactory.getLogger(ValidationError.class)
            .warn(
                "Failed building error message '{}' for locale {} with {} args",
                messageKey,
                locale,
                messageArgs.size(),
                ex);
        return messageKey;
      }
    };
  }

  static ValidationError customAspectConflict(
      String caType, Domain domain, List<Domain> conflictingDomains, Element element) {
    return localized(
        "error_custom_aspect_conflict",
        List.of(
            l -> domain.getTranslations(l).getName(),
            _ -> domain.getTemplateVersion(),
            l -> formatConflictingDomains(conflictingDomains, l),
            l -> formatConflictingAttributes(element, caType, domain, conflictingDomains, l)));
  }

  static void throwOnErrors(Collection<ValidationError> errors) {
    if (!errors.isEmpty()) {
      throw new UnprocessableDataException(
          errors.stream().map(e -> e.getMessage(Locale.ENGLISH)).collect(Collectors.joining(", ")));
    }
  }

  static List<ValidationError> mergeIfAny(
      ValidationError summary, Collection<ValidationError> innerErrors) {
    if (innerErrors.isEmpty()) {
      return new ArrayList<>();
    }
    return List.of(concat(List.of(summary, concat(innerErrors, ", ")), ": "));
  }

  static ValidationError concat(Collection<ValidationError> errors, String glue) {
    return locale ->
        errors.stream().map(e -> e.getMessage(locale)).collect(Collectors.joining(glue));
  }

  private static String formatConflictingDomains(
      Collection<Domain> conflictingDomains, Locale locale) {
    return String.join(
        ", ", conflictingDomains.stream().map(d -> d.getTranslations(locale).getName()).toList());
  }

  private static String formatConflictingAttributes(
      Element element,
      String caType,
      Domain domain,
      List<Domain> conflictingDomains,
      Locale locale) {
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
