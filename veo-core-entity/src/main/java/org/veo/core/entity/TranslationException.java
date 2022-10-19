/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Jonas Jordan
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

import static java.util.Comparator.naturalOrder;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;

import java.util.List;

import org.veo.core.entity.specification.TranslationValidator;
import org.veo.core.entity.specification.TranslationValidator.Violation;

/**
 * Exception while parsing translations. Lists all violations of translations grouped by language
 * and reason.
 */
public class TranslationException extends DomainException {

  public TranslationException(String msg) {
    super(msg);
  }

  public TranslationException(List<Violation> issues) {
    super(
        "Issues were found in the translations: "
            + issues.stream().collect(groupingBy(Violation::language)).entrySet().stream()
                .map(group -> printViolationsForLanguage(group.getKey(), group.getValue()))
                .sorted(naturalOrder())
                .collect(joining("    /    ")));
  }

  private static String printViolationsForLanguage(String lang, List<Violation> issues) {
    var violationTexts =
        issues.stream().collect(groupingBy(Violation::reason)).entrySet().stream()
            .map(group -> printViolationsForReason(group.getKey(), group.getValue()))
            .sorted(naturalOrder())
            .toList();
    return "Language '%s': %s".formatted(lang, String.join(" ; ", violationTexts));
  }

  private static String printViolationsForReason(
      TranslationValidator.Reason reason, List<Violation> issues) {
    var keys = issues.stream().map(Violation::key).sorted((naturalOrder())).toList();
    return "%s: %s".formatted(reason, String.join(", ", keys));
  }
}
