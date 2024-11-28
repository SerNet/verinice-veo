/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Jochen Kemnade
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
package org.veo.core.usecase.domain;

import java.util.Set;
import java.util.UUID;

import jakarta.validation.Valid;

import org.veo.core.repository.DomainRepository;
import org.veo.core.service.MigrateDomainUseCase;
import org.veo.core.usecase.MigrationFailedException;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.UseCase.EmptyOutput;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class UpdateAllClientDomainsUseCase
    implements UseCase<UpdateAllClientDomainsUseCase.InputData, EmptyOutput> {

  private final DomainRepository domainRepository;
  private final MigrateDomainUseCase migrateDomainUseCase;

  @Override
  public EmptyOutput execute(InputData input) {
    Set<UUID> newDomainIds = domainRepository.findIdsByTemplateId(input.domainTemplateId);
    int count = newDomainIds.size();
    log.info("Migrating {} clients to new domain template {}", count, input.domainTemplateId);
    int migrationsDone = 0;
    for (UUID newDomainId : newDomainIds) {
      try {
        migrateDomainUseCase.execute(new MigrateDomainUseCase.InputData(newDomainId));
        migrationsDone++;
        log.info("{} of {} migrations performed", migrationsDone, count);
      } catch (MigrationFailedException e) {
        log.error("Error migrating {} units to domain {}", e.getFailureCount(), newDomainId, e);
      }
    }

    if (migrationsDone != newDomainIds.size()) {
      throw MigrationFailedException.forClient(
          newDomainIds.size(), newDomainIds.size() - migrationsDone);
    }
    return EmptyOutput.INSTANCE;
  }

  @Valid
  public record InputData(UUID domainTemplateId) implements UseCase.InputData {}
}
