/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2019  Alexander Koderman.
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

import static java.lang.String.format;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import jakarta.validation.Valid;

import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Unit;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.entity.exception.ReferenceTargetNotFoundException;
import org.veo.core.entity.transform.EntityFactory;
import org.veo.core.repository.ClientRepository;
import org.veo.core.repository.DomainRepository;
import org.veo.core.repository.UnitRepository;
import org.veo.core.usecase.RetryableUseCase;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.common.NameableInputData;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Create a new unit for a client. If a parentId is given, the unit will be created as a subunit of
 * an existing unit.
 *
 * <p>If no client exists for the given client-ID it will be created. Users of this class must
 * ensure that a clientID belongs to a valid client - i.e. this class is NOT the authoritative
 * source to determine if a clientID is valid or not.
 *
 * <p>Instead, this task should be carried out by an authentication service which provides a valid
 * clientID to this use case.
 *
 * @author akoderman
 */
@RequiredArgsConstructor
@Slf4j
public class CreateUnitUseCase
    implements TransactionalUseCase<CreateUnitUseCase.InputData, CreateUnitUseCase.OutputData>,
        RetryableUseCase {

  private final ClientRepository clientRepository;
  private final UnitRepository unitRepository;
  private final DomainRepository domainRepository;
  private final EntityFactory entityFactory;

  @Override
  public OutputData execute(InputData input) {
    Client client =
        clientRepository
            .findById(input.clientId)
            .orElseThrow(() -> new NotFoundException(input.clientId, Client.class));

    // Note: the new client will get the name of the new unit by default.
    // If we want to get a client name we would have to do a REST call to
    // get it
    // from the auth server. Alternatively, the auth server could publish a
    // name
    // change event
    // which we listen to. This would require messaging middleware.
    client.incrementTotalUnits(input.maxUnits);
    Unit newUnit;
    if (input.parentUnitId.isEmpty()) {
      newUnit = entityFactory.createUnit(input.nameableInput.name(), null);
    } else {
      Unit parentUnit =
          unitRepository
              .findById(input.parentUnitId.get())
              .orElseThrow(
                  () ->
                      new ReferenceTargetNotFoundException(
                          format("Parent unit %s was not found", input.parentUnitId.get())));
      newUnit = entityFactory.createUnit(input.nameableInput.name(), parentUnit);
    }

    newUnit.setAbbreviation(input.nameableInput.abbreviation());
    newUnit.setDescription(input.nameableInput.description());
    newUnit.setClient(client);
    if (input.domainIds != null) {
      try {
        Set<Domain> domains = domainRepository.getByIds(input.domainIds, client.getId());
        newUnit.addToDomains(domains);
      } catch (NotFoundException e) {
        throw new ReferenceTargetNotFoundException(e.getMessage());
      }
    }
    Unit save = unitRepository.save(newUnit);

    return new OutputData(save);
  }

  @Override
  public Isolation getIsolation() {
    return Isolation.REPEATABLE_READ;
  }

  @Override
  public int getMaxAttempts() {
    return 5;
  }

  @Override
  public boolean isReadOnly() {
    return false;
  }

  @Valid
  public record InputData(
      NameableInputData nameableInput,
      UUID clientId,
      Optional<UUID> parentUnitId,
      Integer maxUnits,
      Set<UUID> domainIds)
      implements UseCase.InputData {}

  @Valid
  public record OutputData(@Valid Unit unit) implements UseCase.OutputData {}
}
