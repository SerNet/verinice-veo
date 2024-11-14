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
package org.veo.core.usecase;

import java.util.List;
import java.util.stream.Collectors;

import com.github.zafarkhaja.semver.Version;

import org.veo.core.entity.BreakingChange;
import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainTemplate;
import org.veo.core.entity.exception.UnprocessableDataException;
import org.veo.core.repository.DomainTemplateRepository;
import org.veo.core.usecase.common.DomainDiff;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DomainChangeService {
  private final DomainTemplateRepository domainTemplateRepository;

  /**
   * Evaluate changes the domain made from its template by validating its migration steps and
   * determining versioning information for creating a new template from the domain.
   *
   * @throws UnprocessableDataException for invalid migrations
   */
  public DomainChangeEvaluation evaluateChanges(Domain domain) {
    if (domain.getDomainTemplate() == null) {
      if (!domain.getDomainMigrationDefinition().migrations().isEmpty()) {
        throw new UnprocessableDataException(
            "Migrations must be empty, because the domain is not based on a template.");
      }
      return new DomainChangeEvaluation(false);
    }
    var newBreakingChanges =
        DomainDiff.determineBreakingChanges(domain, domain.getDomainTemplate());
    if (!newBreakingChanges.isEmpty()) {
      validate(domain, domain.getDomainTemplate(), newBreakingChanges);
      return new DomainChangeEvaluation(true);
    }
    var previousMajor = Version.parse(domain.getTemplateVersion()).majorVersion() - 1;
    var previousMajorTemplate =
        domainTemplateRepository.findLatestByMajor(domain.getName(), previousMajor).orElse(null);
    if (previousMajorTemplate != null) {
      validate(
          domain,
          previousMajorTemplate,
          DomainDiff.determineBreakingChanges(domain, previousMajorTemplate));
    } else if (!domain.getDomainMigrationDefinition().migrations().isEmpty()) {
      throw new UnprocessableDataException(
          "Migrations must be empty, because no breaking changes from domain template %s were detected and no previous major version template (%s.*.*) was found."
              .formatted(domain.getTemplateVersion(), previousMajor));
    }
    return new DomainChangeEvaluation(false);
  }

  private static void validate(
      Domain domain, DomainTemplate templateToMigrateFrom, List<BreakingChange> breakingChanges) {
    try {
      domain.getDomainMigrationDefinition().validate(domain, templateToMigrateFrom);
      var unhandledChanges =
          breakingChanges.stream()
              .filter(
                  breakingChange ->
                      domain.getDomainMigrationDefinition().migrations().stream()
                          .noneMatch(m -> m.handles(breakingChange)))
              .toList();
      if (!unhandledChanges.isEmpty()) {
        throw new UnprocessableDataException(
            "Missing migration steps: %s"
                .formatted(
                    unhandledChanges.stream()
                        .map(Record::toString)
                        .collect(Collectors.joining(", "))));
      }
    } catch (UnprocessableDataException ex) {
      throw new UnprocessableDataException(
          "Migration definition not suited to update from old domain template %s: %s"
              .formatted(templateToMigrateFrom.getTemplateVersion(), ex.getMessage()));
    }
  }

  public record DomainChangeEvaluation(
      // TODO #3315 This controls whether the domain must be released as a major template. Once we
      // are able to detect any structural changes in the domain, we can also differentiate between
      // patch and minor level changes and confidentially return the required new version number
      // here, which means that content creators no longer have to define version numbers manually.
      boolean introducesBreakingChanges) {}
}
