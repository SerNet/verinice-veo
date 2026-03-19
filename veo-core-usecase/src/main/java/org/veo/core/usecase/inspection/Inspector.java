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
package org.veo.core.usecase.inspection;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.zafarkhaja.semver.Version;

import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;
import org.veo.core.entity.inspection.Finding;
import org.veo.core.repository.DomainTemplateRepository;
import org.veo.core.usecase.DomainChangeService;
import org.veo.core.usecase.TemplateItems;
import org.veo.core.usecase.base.DomainSensitiveElementValidator;
import org.veo.core.usecase.service.DomainTemplateService;

import lombok.RequiredArgsConstructor;

/** Runs all applicable inspections on an element (in the context of a domain). */
@RequiredArgsConstructor
public class Inspector {
  private final DomainTemplateRepository domainTemplateRepository;
  private final DomainTemplateService domainTemplateService;
  private final DomainChangeService domainChangeService;

  public Set<Finding> inspect(Element element, Domain domain) {
    return Stream.concat(
            domain.getInspections().values().stream()
                .map(inspection -> inspection.run(element, domain))
                .filter(Optional::isPresent)
                .map(Optional::get),
            getMigrationFindings(element, domain).stream())
        .collect(Collectors.toSet());
  }

  private Collection<Finding> getMigrationFindings(Element element, Domain domain) {
    Version currentVersion = Version.parse(domain.getTemplateVersion());
    return domainTemplateRepository
        .findLatestBetween(
            // Both major and minor updates can cause conflicts. It is not possible to update
            // directly to the major version after next.
            domain.getName(),
            currentVersion.nextMinorVersion(),
            currentVersion.nextMajorVersion().nextMinorVersion(Long.MAX_VALUE))
        .map(
            majorUpdate -> {
              // perform domain update dry run and report errors as warnings
              var tempDomain =
                  domainTemplateService.createDomain(
                      element.getOwner().getClient(), majorUpdate.getId(), TemplateItems.NONE);
              domainChangeService.transferCustomization(domain, tempDomain);
              tempDomain.migrate(List.of(element), domain);
              return DomainSensitiveElementValidator.getErrors(element, tempDomain).stream()
                  .map(e -> e.toDomainUpdateFinding(tempDomain))
                  .toList();
            })
        .orElse(Collections.emptyList());
  }
}
