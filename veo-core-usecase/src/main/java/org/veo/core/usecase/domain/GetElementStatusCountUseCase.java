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

import javax.validation.Valid;

import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;
import org.veo.core.entity.EntityType;
import org.veo.core.entity.Key;
import org.veo.core.entity.Unit;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.entity.specification.ClientBoundaryViolationException;
import org.veo.core.entity.statistics.ElementStatusCounts;
import org.veo.core.repository.DomainRepository;
import org.veo.core.repository.RepositoryProvider;
import org.veo.core.repository.SubTypeStatusCount;
import org.veo.core.repository.UnitRepository;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;

import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class GetElementStatusCountUseCase
    implements TransactionalUseCase<
        GetElementStatusCountUseCase.InputData, GetElementStatusCountUseCase.OutputData> {

  private final DomainRepository domainRepository;
  private final UnitRepository unitRepository;
  private final RepositoryProvider repositoryProvider;

  @Override
  public OutputData execute(InputData input) {
    Domain domain = domainRepository.getById(input.getDomainId());
    Client client = input.getAuthenticatedClient();
    if (!client.equals(domain.getOwner())) {
      throw new ClientBoundaryViolationException(domain, client);
    }
    if (!domain.isActive()) {
      throw new NotFoundException("Domain is inactive.");
    }
    Unit unit = unitRepository.getById(input.getUnitId());
    unit.checkSameClient(input.getAuthenticatedClient());

    ElementStatusCounts elementStatusCounts = new ElementStatusCounts(domain);

    EntityType.ELEMENT_TYPES.stream()
        .forEach(
            type -> {
              String typeName = type.getSingularTerm();
              log.debug("Checking type {}", typeName);
              Set<SubTypeStatusCount> counts =
                  repositoryProvider
                      .getElementRepositoryFor((Class<? extends Element>) type.getType())
                      .getCountsBySubType(unit, domain);
              log.debug("Found counts: {}", counts);

              counts.forEach(
                  c ->
                      elementStatusCounts.setCount(
                          type, c.getSubType(), c.getStatus(), c.getCount()));
            });

    return new OutputData(elementStatusCounts);
  }

  @Valid
  @Value
  public static class InputData implements UseCase.InputData {
    private final Key<UUID> unitId;
    private final Key<UUID> domainId;
    private final Client authenticatedClient;
  }

  @Valid
  @Value
  public static class OutputData implements UseCase.OutputData {
    @Valid ElementStatusCounts result;
  }
}
