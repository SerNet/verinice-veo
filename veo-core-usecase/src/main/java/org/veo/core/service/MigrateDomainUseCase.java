/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Jonas Jordan
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
package org.veo.core.service;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.transaction.Transactional;

import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;
import org.veo.core.entity.EntityType;
import org.veo.core.entity.Key;
import org.veo.core.entity.Unit;
import org.veo.core.repository.DomainRepository;
import org.veo.core.repository.RepositoryProvider;
import org.veo.core.repository.UnitRepository;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.decision.Decider;
import org.veo.service.ElementMigrationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Migrates to target domain, assuming that there is one active older version of the domain in the
 * client. Moves all elements associated with the old domain to the new domain and deactivates the
 * old domain.
 */
@RequiredArgsConstructor
@Slf4j
public class MigrateDomainUseCase
    implements TransactionalUseCase<MigrateDomainUseCase.InputData, UseCase.EmptyOutput> {
  private final DomainRepository domainRepository;
  private final RepositoryProvider repositoryProvider;
  private final ElementMigrationService elementMigrationService;
  private final Decider decider;
  private final UnitRepository unitRepository;

  @Override
  public boolean isReadOnly() {
    return false;
  }

  // TODO #2338 remove @Transactional (currently, without this annotation there would be no
  // transaction when calling this from another use case)
  @Transactional
  public EmptyOutput execute(InputData input) {
    var newDomain = domainRepository.getById(input.domainId);
    Client client = newDomain.getOwner();

    Set<Domain> clientActiveDomains =
        client.getDomains().stream()
            .filter(Domain::isActive)
            .filter(d -> d.getName().equals(newDomain.getName()))
            .collect(Collectors.toSet());
    if (clientActiveDomains.size() != 2) {
      log.warn(
          "Skipping client {}, found {} active domains instead of 2",
          client,
          clientActiveDomains.size());
      return EmptyOutput.INSTANCE;
    }
    Domain domainToUpdate =
        clientActiveDomains.stream()
            .filter(Predicate.not(newDomain::equals))
            .findAny()
            .orElseThrow();
    performMigration(client, domainToUpdate, newDomain);
    domainToUpdate.setActive(false);
    return EmptyOutput.INSTANCE;
  }

  private void performMigration(Client client, Domain domainToUpdate, Domain newDomain) {
    log.info(
        "Performing migration for domain {}->{} (client {})",
        domainToUpdate,
        newDomain,
        client.getIdAsString());
    List<Unit> unitsToUpdate = unitRepository.findByClient(client);
    for (Unit unit : unitsToUpdate) {
      if (unit.removeFromDomains(domainToUpdate)) {
        unit.addToDomains(newDomain);
      }
    }

    var elements =
        EntityType.ELEMENT_TYPES.stream()
            .map(t -> (Class<Element>) t.getType())
            .map(repositoryProvider::getElementRepositoryFor)
            .flatMap(repo -> repo.findByDomain(domainToUpdate).stream())
            .toList();

    log.info(
        "Transferring domain-specific information on {} elements from old domain to new domain",
        elements.size());
    elements.forEach(element -> element.transferToDomain(domainToUpdate, newDomain));

    // Mercilessly remove all information from the elements that is no longer valid under the new
    // domain. This must happen after all elements have been transferred, because link targets are
    // also validated and must have been transferred beforehand.
    elements.forEach(element -> elementMigrationService.migrate(element, newDomain));

    elements.forEach(
        element -> element.setDecisionResults(decider.decide(element, newDomain), newDomain));
  }

  public record InputData(Key<UUID> domainId) implements UseCase.InputData {}
}
