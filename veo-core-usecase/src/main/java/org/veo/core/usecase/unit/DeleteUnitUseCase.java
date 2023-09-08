/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Alexander Koderman.
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

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.veo.core.entity.Client;
import org.veo.core.entity.Element;
import org.veo.core.entity.Key;
import org.veo.core.entity.Unit;
import org.veo.core.repository.ClientRepository;
import org.veo.core.repository.ElementQuery;
import org.veo.core.repository.GenericElementRepository;
import org.veo.core.repository.PagingConfiguration;
import org.veo.core.repository.UnitRepository;
import org.veo.core.usecase.RetryableUseCase;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.UseCase.EmptyOutput;

import lombok.RequiredArgsConstructor;
import lombok.Value;

@RequiredArgsConstructor
public class DeleteUnitUseCase
    implements TransactionalUseCase<DeleteUnitUseCase.InputData, EmptyOutput>, RetryableUseCase {

  private final ClientRepository clientRepository;
  private final UnitRepository unitRepository;
  private final GenericElementRepository genericElementRepository;

  @Override
  public EmptyOutput execute(InputData input) {
    Client client = clientRepository.getById(input.getAuthenticatedClient().getId());
    Unit unit = unitRepository.getById(input.unitId);
    unit.checkSameClient(client);

    removeObjectsInUnit(unit);
    unitRepository.delete(unit);
    client.decrementTotalUnits();
    return EmptyOutput.INSTANCE;
  }

  void removeObjectsInUnit(Unit unit) {

    ElementQuery<Element> query = genericElementRepository.query(unit.getClient());
    query.whereOwnerIs(unit);
    query.fetchAppliedCatalogItems();
    query.fetchParentsAndChildrenAndSiblings();
    query.fetchRisks();
    query.fetchRiskValuesAspects();
    query.fetchControlImplementations();
    query.fetchRequirementImplementations();

    List<Element> entitiesInUnit = query.execute(PagingConfiguration.UNPAGED).getResultPage();

    genericElementRepository.deleteAll(entitiesInUnit);
  }

  @Override
  public boolean isReadOnly() {
    return false;
  }

  @Override
  public Isolation getIsolation() {
    return Isolation.REPEATABLE_READ;
  }

  @Override
  public int getMaxAttempts() {
    return 5;
  }

  @Valid
  @Value
  public static class InputData implements UseCase.InputData {
    Key<UUID> unitId;
    Client authenticatedClient;
  }
}
