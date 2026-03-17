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
package org.veo.core.usecase.service;

import java.util.HashSet;
import java.util.Set;

import org.veo.core.entity.Domain;
import org.veo.core.entity.Unit;
import org.veo.core.repository.GenericElementRepository;
import org.veo.core.repository.PagingConfiguration;
import org.veo.core.usecase.DomainUpdateFailedException;
import org.veo.core.usecase.base.DomainSensitiveElementValidator;
import org.veo.core.usecase.decision.Decider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class UnitMigrationService {
  private final GenericElementRepository genericElementRepository;
  private final Decider decider;

  public void update(Unit unit, Domain oldDomain, Domain newDomain)
      throws DomainUpdateFailedException {

    log.info(
        "Performing migration for domain {}::{}->{} (unit {})",
        newDomain.getName(),
        oldDomain.getTemplateVersion(),
        newDomain.getTemplateVersion(),
        unit.getId());

    var elementQuery = genericElementRepository.query(unit.getClient());
    elementQuery.whereUnitIn(Set.of(unit));
    elementQuery.whereDomainsContain(oldDomain);
    var elements = elementQuery.execute(PagingConfiguration.UNPAGED).resultPage();
    unit.addToDomains(newDomain);

    newDomain.migrate(elements, oldDomain);

    elements.forEach(
        element -> element.setDecisionResults(decider.decide(element, newDomain), newDomain));
    var invalidElements =
        elements.stream()
            .filter(e -> !DomainSensitiveElementValidator.isValid(e, newDomain))
            .toList();

    if (!invalidElements.isEmpty()) {
      // Conflicted elements are left associated with the old domain to allow for nicer error
      // reporting.
      throw new DomainUpdateFailedException(oldDomain, new HashSet<>(elements));
    }
    log.debug("removing elements from old domain: {}", oldDomain);
    elements.forEach(e -> e.removeFromDomains(oldDomain));
    unit.removeFromDomains(oldDomain);
  }
}
