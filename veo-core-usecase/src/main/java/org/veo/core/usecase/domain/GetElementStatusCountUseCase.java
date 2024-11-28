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

import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.core.entity.EntityType;
import org.veo.core.entity.Unit;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.entity.specification.ClientBoundaryViolationException;
import org.veo.core.entity.statistics.ElementStatusCounts;
import org.veo.core.repository.DomainRepository;
import org.veo.core.repository.GenericElementRepository;
import org.veo.core.repository.SubTypeStatusCount;
import org.veo.core.repository.UnitRepository;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class GetElementStatusCountUseCase
    implements TransactionalUseCase<
        GetElementStatusCountUseCase.InputData, GetElementStatusCountUseCase.OutputData> {

  private final DomainRepository domainRepository;
  private final UnitRepository unitRepository;
  private final GenericElementRepository genericElementRepository;

  @Override
  public OutputData execute(InputData input) {
    Domain domain = domainRepository.getById(input.domainId);
    Client client = input.authenticatedClient;
    if (!client.equals(domain.getOwner())) {
      throw new ClientBoundaryViolationException(domain, client);
    }
    if (!domain.isActive()) {
      throw new NotFoundException("Domain is inactive.");
    }
    Unit unit = unitRepository.getById(input.unitId);
    unit.checkSameClient(input.authenticatedClient);

    ElementStatusCounts elementStatusCounts = new ElementStatusCounts(domain);

    Set<SubTypeStatusCount> counts = genericElementRepository.getCountsBySubType(unit, domain);
    log.debug("Found counts: {}", counts);

    counts.forEach(
        c -> {
          String type = c.getType();
          String subType = c.getSubType();
          if (!domain.getElementTypeDefinition(type).getSubTypes().containsKey(subType)) {
            log.error(
                "Unit {} ({}) contains elements with an invalid subType {} in domain {} ({})",
                unit.getName(),
                unit.getIdAsString(),
                subType,
                domain.getName(),
                domain.getIdAsString());
            return;
          }
          elementStatusCounts.setCount(
              EntityType.getBySingularTerm(type), subType, c.getStatus(), c.getCount());
        });

    return new OutputData(elementStatusCounts);
  }

  @Valid
  public record InputData(UUID unitId, UUID domainId, Client authenticatedClient)
      implements UseCase.InputData {}

  @Valid
  public record OutputData(@Valid ElementStatusCounts result) implements UseCase.OutputData {}
}
