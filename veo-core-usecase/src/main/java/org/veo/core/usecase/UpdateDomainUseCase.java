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
package org.veo.core.usecase;

import java.util.HashSet;
import java.util.UUID;

import org.veo.core.UserAccessRights;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;
import org.veo.core.repository.DomainRepository;
import org.veo.core.repository.UnitRepository;
import org.veo.core.usecase.service.DomainTemplateService;
import org.veo.core.usecase.service.UnitMigrationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class UpdateDomainUseCase
    implements TransactionalUseCase<UpdateDomainUseCase.InputData, UpdateDomainUseCase.OutputData> {
  private final DomainRepository domainRepository;
  private final UnitRepository unitRepository;
  private final DomainTemplateService domainTemplateService;
  private final DomainChangeService domainChangeService;
  private final UnitMigrationService unitMigrationService;

  @Override
  public boolean isReadOnly() {
    return false;
  }

  @Override
  public OutputData execute(InputData input, UserAccessRights userAccessRights) {
    var oldDomain = domainRepository.getActiveById(input.domainId, userAccessRights.getClientId());
    var newDomain =
        domainTemplateService.createDomain(oldDomain.getOwner(), input.domainTemplateId, true);
    domainChangeService.transferCustomization(oldDomain, newDomain);
    migrateUnits(oldDomain, newDomain);
    oldDomain.setActive(false);
    return new OutputData(newDomain);
  }

  private void migrateUnits(Domain oldDomain, Domain newDomain) throws DomainUpdateFailedException {
    var conflictedElements = new HashSet<Element>();
    var units = unitRepository.findByDomain(oldDomain.getId());
    units.forEach(
        u -> {
          try {
            unitMigrationService.update(u, oldDomain, newDomain);
          } catch (DomainUpdateFailedException ex) {
            conflictedElements.addAll(ex.getConflictedElements());
          }
        });
    if (!conflictedElements.isEmpty()) {
      throw new DomainUpdateFailedException(oldDomain, conflictedElements);
    }
  }

  public record InputData(UUID domainId, UUID domainTemplateId) implements UseCase.InputData {}

  public record OutputData(Domain newDomain) implements UseCase.OutputData {}
}
