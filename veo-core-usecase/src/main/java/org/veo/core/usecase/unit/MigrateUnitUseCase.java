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
package org.veo.core.usecase.unit;

import java.util.Set;
import java.util.UUID;

import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import org.veo.core.entity.Key;
import org.veo.core.repository.DomainRepository;
import org.veo.core.repository.GenericElementRepository;
import org.veo.core.repository.PagingConfiguration;
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
public class MigrateUnitUseCase
    implements TransactionalUseCase<MigrateUnitUseCase.InputData, UseCase.EmptyOutput> {
  private final DomainRepository domainRepository;
  private final ElementMigrationService elementMigrationService;
  private final GenericElementRepository genericElementRepository;
  private final Decider decider;
  private final UnitRepository unitRepository;

  @Override
  public boolean isReadOnly() {
    return false;
  }

  // TODO #2338 remove @Transactional (currently, without this annotation there would be no
  // transaction when calling this from another use case)
  @Transactional(TxType.REQUIRES_NEW)
  public EmptyOutput execute(InputData input) {
    var unit = unitRepository.getById(input.unitId);
    var oldDomain = domainRepository.getById(input.domainIdOld, unit.getClient().getId());
    var newDomain = domainRepository.getById(input.domainIdNew, unit.getClient().getId());

    log.info(
        "Performing migration for domain {} {}->{} (unit {})",
        newDomain.getName(),
        oldDomain.getTemplateVersion(),
        newDomain.getTemplateVersion(),
        unit.getId());

    var elementQuery = genericElementRepository.query(unit.getClient());
    elementQuery.whereUnitIn(Set.of(unit));
    elementQuery.whereDomainsContain(oldDomain);
    var elements = elementQuery.execute(PagingConfiguration.UNPAGED).getResultPage();
    unit.addToDomains(newDomain);

    log.info(
        "Transferring domain-specific information on {} elements from old domain to new domain",
        elements.size());
    elements.forEach(element -> element.transferToDomain(oldDomain, newDomain));

    // Mercilessly remove all information from the elements that is no longer valid under the new
    // domain. This must happen after all elements have been transferred, because link targets are
    // also validated and must have been transferred beforehand.
    elements.forEach(element -> elementMigrationService.migrate(element, newDomain));

    elements.forEach(
        element -> element.setDecisionResults(decider.decide(element, newDomain), newDomain));
    unit.removeFromDomains(oldDomain);
    return EmptyOutput.INSTANCE;
  }

  public record InputData(Key<UUID> unitId, Key<UUID> domainIdOld, Key<UUID> domainIdNew)
      implements UseCase.InputData {}
}
