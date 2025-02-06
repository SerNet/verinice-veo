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
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.transaction.Transactional;

import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.core.entity.ElementType;
import org.veo.core.entity.Unit;
import org.veo.core.entity.riskdefinition.RiskDefinition;
import org.veo.core.repository.DomainRepository;
import org.veo.core.repository.UnitRepository;
import org.veo.core.usecase.MigrationFailedException;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.unit.MigrateUnitUseCase;

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
  private final UnitRepository unitRepository;
  private final MigrateUnitUseCase migrateUnitUseCase;

  @Override
  public boolean isReadOnly() {
    return false;
  }

  // TODO #2338 remove @Transactional (currently, without this annotation there would be no
  // transaction when calling this from another use case)
  @Transactional
  @Override
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
    applyCustomization(domainToUpdate, newDomain);
    migrateUnits(client, domainToUpdate, newDomain);
    domainToUpdate.setActive(false);
    return EmptyOutput.INSTANCE;
  }

  private void migrateUnits(Client client, Domain oldDomain, Domain newDomain) {
    log.info(
        "Performing migration for domain {}->{} (client {})",
        oldDomain,
        newDomain,
        client.getIdAsString());

    List<Unit> unitsToUpdate = unitRepository.findByDomain(oldDomain.getId());
    int failureCount = 0;
    for (Unit unit : unitsToUpdate) {
      try {
        migrateUnitUseCase.execute(
            new MigrateUnitUseCase.InputData(unit.getId(), oldDomain.getId(), newDomain.getId()));
      } catch (Exception e) {
        failureCount++;
        log.error("Error migrating unit {}", unit, e);
      }
    }
    if (failureCount != 0) {
      throw MigrationFailedException.forDomain(unitsToUpdate.size(), failureCount);
    }
  }

  private void applyCustomization(Domain oldDomain, Domain newDomain) {
    // Copy all customized risk definitions to the new domain. This may overwrite risk definition
    // changes from the new domain template version.
    oldDomain
        .getRiskDefinitions()
        .forEach(
            (id, riskDef) -> {
              var originalRiskDefinition =
                  Optional.ofNullable(oldDomain.getDomainTemplate())
                      .flatMap(dt -> dt.getRiskDefinition(id))
                      .orElse(null);
              if (!riskDef.equals(originalRiskDefinition)) {
                log.debug(
                    "Copying customized risk definition {} from {} {} ({}) to new version {} ({})",
                    id,
                    oldDomain.getName(),
                    oldDomain.getTemplateVersion(),
                    oldDomain.getIdAsString(),
                    newDomain.getTemplateVersion(),
                    newDomain.getIdAsString());
                newDomain.applyRiskDefinition(id, migrate(riskDef, newDomain));
              }
            });
  }

  private RiskDefinition migrate(RiskDefinition riskDef, Domain newDomain) {
    return riskDef.withImpactInheritingLinks(
        migrate(riskDef.getImpactInheritingLinks(), newDomain));
  }

  private Map<ElementType, List<String>> migrate(
      Map<ElementType, List<String>> impactInheritingLinks, Domain newDomain) {
    return impactInheritingLinks.entrySet().stream()
        .collect(
            Collectors.toMap(
                Map.Entry::getKey,
                e ->
                    e.getValue().stream()
                        .filter(
                            link ->
                                newDomain
                                    .getElementTypeDefinition(e.getKey())
                                    .findLink(link)
                                    .isPresent())
                        .toList()))
        .entrySet()
        .stream()
        .filter(e -> !e.getValue().isEmpty())
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  public record InputData(UUID domainId) implements UseCase.InputData {}
}
