/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Urs Zeidler
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

import java.util.List;

import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Unit;
import org.veo.core.entity.specification.ClientBoundaryViolationException;
import org.veo.core.repository.DomainRepository;
import org.veo.core.repository.UnitRepository;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase.EmptyOutput;
import org.veo.core.usecase.UseCase.IdAndClient;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DeleteDomainUseCase implements TransactionalUseCase<IdAndClient, EmptyOutput> {

  private final DomainRepository domainRepository;
  private final UnitRepository unitRepository;

  public DeleteDomainUseCase(DomainRepository domainRepository, UnitRepository unitRepository) {
    super();
    this.domainRepository = domainRepository;
    this.unitRepository = unitRepository;
  }

  @Override
  public EmptyOutput execute(IdAndClient input) {
    Domain domain = domainRepository.getById(input.id());
    Client client = input.authenticatedClient();
    if (!client.equals(domain.getOwner())) {
      throw new ClientBoundaryViolationException(domain, client);
    }

    log.info("client.domains {}", client.getDomains().size());
    List<Unit> units = unitRepository.findByClient(client);
    boolean isUsed =
        units.stream()
            .anyMatch(
                u ->
                    u.getDomains().stream()
                        .map(d -> d.getId())
                        .anyMatch(k -> k.equals(input.id())));

    if (isUsed) {
      throw new DomainInUseException("Domain in use: " + input.id());
    }

    log.info("deleting unused domain {}", domain);
    domainRepository.deleteById(domain.getId());
    return EmptyOutput.INSTANCE;
  }

  @Override
  public boolean isReadOnly() {
    return false;
  }
}
