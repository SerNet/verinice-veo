/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Jonas Jordan
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

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.veo.core.entity.AccountProvider;
import org.veo.core.entity.Element;
import org.veo.core.entity.Key;
import org.veo.core.entity.Unit;
import org.veo.core.repository.GenericElementRepository;
import org.veo.core.repository.PagingConfiguration;
import org.veo.core.repository.UnitRepository;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class GetUnitDumpUseCase
    implements TransactionalUseCase<GetUnitDumpUseCase.InputData, GetUnitDumpUseCase.OutputData> {
  private final AccountProvider accountProvider;
  private final GenericElementRepository genericElementRepository;
  private final UnitRepository unitRepository;

  @Override
  public OutputData execute(InputData input) {
    var unit = unitRepository.getById(input.unitId);
    if (!accountProvider.getCurrentUserAccount().isAdmin()) {
      unit.checkSameClient(accountProvider.getCurrentUserAccount().getClient());
    }
    return new OutputData(unit, getElements(unit));
  }

  private Set<Element> getElements(Unit unit) {
    var query = genericElementRepository.query(unit.getClient());
    query.whereUnitIn(Set.of(unit));
    return new HashSet<>(query.execute(PagingConfiguration.UNPAGED).getResultPage());
  }

  @Data
  @AllArgsConstructor
  public static class InputData implements UseCase.InputData {
    private Key<UUID> unitId;
  }

  @Data
  @AllArgsConstructor
  public static class OutputData implements UseCase.OutputData {
    private Unit unit;
    private Set<Element> elements;
  }
}
