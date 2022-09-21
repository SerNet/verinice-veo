/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Jochen Kemnade.
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
package org.veo.core.usecase.base;

import javax.validation.Valid;

import org.veo.core.entity.CompositeElement;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.repository.ElementRepository;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;

import lombok.RequiredArgsConstructor;
import lombok.Value;

@RequiredArgsConstructor
public class GetElementUseCase<T extends CompositeElement<T>>
    implements TransactionalUseCase<UseCase.IdAndClient, GetElementUseCase.OutputData<T>> {

  private final ElementRepository<T> repository;
  private final Class<T> type;

  public OutputData<T> execute(IdAndClient input) {
    T element =
        repository
            .findById(input.getId())
            .orElseThrow(() -> new NotFoundException(input.getId(), type));
    element.checkSameClient(input.getAuthenticatedClient());
    return new OutputData<>(element);
  }

  @Valid
  @Value
  public static class OutputData<T> implements UseCase.OutputData {
    @Valid T element;
  }
}
