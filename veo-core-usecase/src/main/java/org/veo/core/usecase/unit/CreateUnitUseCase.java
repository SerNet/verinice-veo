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

import javax.validation.Valid;

import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Key;
import org.veo.core.entity.Unit;
import org.veo.core.entity.event.ClientEvent.ClientChangeType;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.entity.exception.ReferenceTargetNotFoundException;
import org.veo.core.entity.specification.MaxUnitsExceededException;
import org.veo.core.entity.transform.EntityFactory;
import org.veo.core.repository.ClientRepository;
import org.veo.core.repository.DomainRepository;
import org.veo.core.repository.UnitRepository;
import org.veo.core.usecase.RetryableUseCase;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.common.NameableInputData;
import org.veo.service.DefaultDomainCreator;

import lombok.RequiredArgsConstructor;
import lombok.Value;
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

  public static final Integer DEFAULT_MAX_UNITS = 2;

  private final ClientRepository clientRepository;
  private final UnitRepository unitRepository;
  private final DomainRepository domainRepository;
  private final EntityFactory entityFactory;
  private final DefaultDomainCreator defaultDomainCreator;
  private final CreateDemoUnitUseCase createDemoUnitUseCase;

  @Override
  public OutputData execute(InputData input) {
    Client client =
        clientRepository.findById(input.getClientId()).orElseGet(() -> createNewClient(input));

    // Note: the new client will get the name of the new unit by default.
    // If we want to get a client name we would have to do a REST call to
    // get it
    // from the auth server. Alternatively, the auth server could publish a
    // name
    // change event
    // which we listen to. This would require messaging middleware.

    int effectiveMaxUnits = Optional.ofNullable(input.maxUnits).orElse(DEFAULT_MAX_UNITS);
    if (client.getTotalUnits() + 1 > effectiveMaxUnits) {
      throwTooManyUnits(effectiveMaxUnits);
    }
    Unit newUnit;
    if (input.getParentUnitId().isEmpty()) {
      newUnit = entityFactory.createUnit(input.getNameableInput().getName(), null);
    } else {
      Unit parentUnit =
          unitRepository
              .findById(input.getParentUnitId().get())
              .orElseThrow(
                  () ->
                      new ReferenceTargetNotFoundException(
                          format("Parent unit %s was not found", input.getParentUnitId().get())));
      newUnit = entityFactory.createUnit(input.getNameableInput().getName(), parentUnit);
    }

    newUnit.setAbbreviation(input.getNameableInput().getAbbreviation());
    newUnit.setDescription(input.getNameableInput().getDescription());
    newUnit.setClient(client);
    client.incrementTotalUnits();
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

  private void throwTooManyUnits(int maxUnits) {
    throw new MaxUnitsExceededException(
        "This account may not create more than " + maxUnits + " units");
  }

  private Client createNewClient(InputData input) {
    // TODO: VEO-1817 remove client creation code
    // By default, the client is created with the unit's name, description,
    // and abbreviation:
    Client client =
        entityFactory.createClient(input.getClientId(), input.getNameableInput().getName());
    defaultDomainCreator.addDefaultDomains(client);
    client.updateState(ClientChangeType.ACTIVATION);
    Client savedClient = clientRepository.save(client);
    createDemoUnitForClient(savedClient);
    return savedClient;
  }

  private void createDemoUnitForClient(Client savedClient) {
    // TODO: VEO-1817 remove demo unit creation code
    createDemoUnitUseCase.execute(new CreateDemoUnitUseCase.InputData(savedClient.getId()));
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
  @Value
  public static class InputData implements UseCase.InputData {
    NameableInputData nameableInput;
    Key<UUID> clientId;
    Optional<Key<UUID>> parentUnitId;
    Integer maxUnits;
    Set<Key<UUID>> domainIds;
  }

  @Valid
  @Value
  public static class OutputData implements UseCase.OutputData {
    @Valid Unit unit;
  }
}
