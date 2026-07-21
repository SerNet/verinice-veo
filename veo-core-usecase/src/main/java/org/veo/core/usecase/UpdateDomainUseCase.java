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

import java.util.UUID;

import org.veo.core.UserAccessRights;
import org.veo.core.entity.Domain;
import org.veo.core.repository.DomainRepository;
import org.veo.core.repository.UnitRepository;
import org.veo.core.usecase.service.DomainTemplateService;
import org.veo.core.usecase.service.MigrationService;

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
  private final MigrationService unitMigrationService;
  private final MessageCreator messageCreator;

  @Override
  public boolean isReadOnly() {
    return false;
  }

  @Override
  public OutputData execute(InputData input, UserAccessRights userAccessRights) {
    userAccessRights.checkUnitUpdateAllowed();
    var oldDomain = domainRepository.getActiveById(input.domainId, userAccessRights.getClientId());
    var newDomain =
        domainTemplateService.createDomain(oldDomain.getOwner(), input.domainTemplateId);
    domainChangeService.transferCustomization(oldDomain, newDomain);
    var units = unitRepository.findByDomain(oldDomain.getId());
    unitMigrationService.updateElements(oldDomain, newDomain, userAccessRights.getUsername());
    units.forEach(
        u -> {
          u.addToDomains(newDomain);
          u.removeFromDomains(oldDomain);
        });
    // Adding the domain to the client triggers many inserts and updates.
    // If this was done before the units are migrated and the migration failed, all these inserts
    // and updates would slow down the request, only to be ultimately rolled back anyway due to the
    // exception.
    oldDomain.getOwner().addToDomains(newDomain);
    // TODO #5017 rethink event creation
    messageCreator.createDomainCreationMessage(newDomain);
    oldDomain.setActive(false);
    return new OutputData(newDomain);
  }

  public record InputData(UUID domainId, UUID domainTemplateId) implements UseCase.InputData {}

  public record OutputData(Domain newDomain) implements UseCase.OutputData {}
}
