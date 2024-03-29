/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Jonas Jordan
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

import java.util.Set;

import org.veo.core.entity.Client;
import org.veo.core.entity.Element;
import org.veo.core.entity.Unit;
import org.veo.core.entity.exception.UnprocessableDataException;
import org.veo.core.repository.UnitRepository;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.base.DomainSensitiveElementValidator;
import org.veo.core.usecase.domain.ElementBatchCreator;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UnitImportUseCase
    implements TransactionalUseCase<UnitImportUseCase.InputData, UnitImportUseCase.OutputData> {
  private final UnitRepository unitRepository;
  private final ElementBatchCreator elementBatchCreator;

  @Override
  public OutputData execute(InputData input) {
    input.unit.setClient(input.client);
    elementBatchCreator.create(input.elements, unitRepository.save(input.unit), false);
    try {
      input.elements.forEach(DomainSensitiveElementValidator::validate);
    } catch (IllegalArgumentException illEx) {
      throw new UnprocessableDataException(illEx.getMessage());
    }
    return new OutputData(input.unit);
  }

  @Override
  public boolean isReadOnly() {
    return false;
  }

  @Data
  @AllArgsConstructor
  public static class InputData implements UseCase.InputData {
    private Client client;
    private Unit unit;
    private Set<Element> elements;
  }

  @Data
  @AllArgsConstructor
  public static class OutputData implements UseCase.OutputData {
    private Unit unit;
  }
}
