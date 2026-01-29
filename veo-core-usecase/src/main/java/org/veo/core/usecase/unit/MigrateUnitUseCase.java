/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2024  Urs Zeidler
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

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import org.veo.core.UserAccessRights;
import org.veo.core.entity.Element;
import org.veo.core.repository.DomainRepository;
import org.veo.core.repository.UnitRepository;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.service.UnitMigrationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * TODO #4517 remove
 *
 * <p>Migrates to target domain, assuming that there is one active older version of the domain in
 * the client. Moves all elements associated with the old domain to the new domain and deactivates
 * the old domain.
 */
@RequiredArgsConstructor
@Slf4j
public class MigrateUnitUseCase
    implements TransactionalUseCase<MigrateUnitUseCase.InputData, MigrateUnitUseCase.OutputData> {
  private final DomainRepository domainRepository;
  private final UnitRepository unitRepository;
  private final UnitMigrationService unitMigrationService;

  @Override
  public boolean isReadOnly() {
    return false;
  }

  // TODO #2338 remove @Transactional (currently, without this annotation there would be no
  // transaction when calling this from another use case)
  @Transactional(TxType.REQUIRES_NEW)
  @Override
  public OutputData execute(InputData input, UserAccessRights userAccessRights) {
    var unit = unitRepository.getById(input.unitId);
    var oldDomain = domainRepository.getById(input.domainIdOld, unit.getClient().getId());
    var newDomain = domainRepository.getById(input.domainIdNew, unit.getClient().getId());
    unitMigrationService.update(unit, oldDomain, newDomain);
    return new OutputData(Collections.emptyList());
  }

  public record InputData(UUID unitId, UUID domainIdOld, UUID domainIdNew)
      implements UseCase.InputData {}

  public record OutputData(List<Element> skipedElements) implements UseCase.OutputData {}
}
