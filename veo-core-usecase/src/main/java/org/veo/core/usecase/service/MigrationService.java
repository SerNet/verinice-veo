/*
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
 */
package org.veo.core.usecase.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;
import org.veo.core.entity.event.ClientOwnedEntityVersioningEvent;
import org.veo.core.entity.event.VersioningEvent;
import org.veo.core.repository.GenericElementRepository;
import org.veo.core.repository.PagingConfiguration;
import org.veo.core.usecase.DomainUpdateFailedException;
import org.veo.core.usecase.MessageCreator;
import org.veo.core.usecase.base.DomainSensitiveElementValidator;
import org.veo.core.usecase.decision.Decider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class MigrationService {
  private final GenericElementRepository genericElementRepository;
  private final Decider decider;
  private final MessageCreator messageCreator;

  public void updateElements(Domain oldDomain, Domain newDomain, String username)
      throws DomainUpdateFailedException {

    log.info(
        "Performing migration for domain {}::{}->{}",
        newDomain.getName(),
        oldDomain.getTemplateVersion(),
        newDomain.getTemplateVersion());

    var elementQuery = genericElementRepository.query(oldDomain.getOwner());
    elementQuery.whereDomainsContain(oldDomain);
    elementQuery.fetchAppliedCatalogItems();
    elementQuery.fetchRisks();
    elementQuery.fetchRiskValuesAspects();
    elementQuery.fetchControlImplementations();
    elementQuery.fetchChildren();
    elementQuery.fetchRequirementImplementations();

    var elements = elementQuery.execute(PagingConfiguration.UNPAGED).resultPage();
    List<Element> invalidElements = new ArrayList<>();
    List<Element> validElements = new ArrayList<>();

    for (Element element : elements) {
      if (!DomainSensitiveElementValidator.isValidForUpdate(element, oldDomain, newDomain)) {
        invalidElements.add(element);
      } else {
        validElements.add(element);
      }
    }

    newDomain.migrate(validElements, oldDomain);

    validElements.forEach(element -> decider.decide(element, newDomain));
    invalidElements.addAll(
        validElements.stream()
            .filter(e -> !DomainSensitiveElementValidator.isValid(e, newDomain))
            .toList());

    if (!invalidElements.isEmpty()) {
      // Conflicted elements are left associated with the old domain to allow for nicer error
      // reporting.
      throw new DomainUpdateFailedException(oldDomain, new HashSet<>(invalidElements));
    }
    log.debug("removing elements from old domain: {}", oldDomain);
    elements.forEach(
        e -> {
          e.removeFromDomains(oldDomain);
          e.setUpdatedAt(Instant.now());
          e.setUpdatedBy(username);
          // TODO #5017 rethink event creation
          messageCreator.createEntityRevisionMessage(
              new ClientOwnedEntityVersioningEvent<>(
                  e,
                  VersioningEvent.ModificationType.UPDATE,
                  e.getUpdatedBy(),
                  e.getUpdatedAt(),
                  e.nextChangeNumberForUpdate()));
        });
  }
}
