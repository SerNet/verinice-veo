/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2024  Aziz Khalledi
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
package org.veo.core.usecase.compliance;

import java.util.Optional;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.veo.core.entity.Client;
import org.veo.core.entity.Key;
import org.veo.core.entity.compliance.ControlImplementation;
import org.veo.core.repository.ControlImplementationQuery;
import org.veo.core.repository.ControlImplementationRepository;
import org.veo.core.repository.PagedResult;
import org.veo.core.repository.PagingConfiguration;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;

import lombok.RequiredArgsConstructor;

/** Return all {@link ControlImplementation}s for the given {@link org.veo.core.entity.Control}. */
@RequiredArgsConstructor
public class GetControlImplementationsUseCase
    implements TransactionalUseCase<
        GetControlImplementationsUseCase.InputData, GetControlImplementationsUseCase.OutputData> {

  private final ControlImplementationRepository controlImplementationRepository;

  @Override
  public OutputData execute(InputData input) {
    var query = controlImplementationRepository.query(input.authenticatedClient, input.domainId);
    applyAdditionalQueryParameters(input, query);
    PagedResult<ControlImplementation> result = query.execute(input.pagingConfiguration);
    return new OutputData(result);
  }

  private void applyAdditionalQueryParameters(InputData input, ControlImplementationQuery query) {
    Optional.ofNullable(input.control).ifPresent(query::whereControlIdIn);
  }

  @Valid
  public record InputData(
      @NotNull Client authenticatedClient,
      Key<UUID> control,
      @NotNull Key<UUID> domainId,
      @NotNull PagingConfiguration pagingConfiguration)
      implements UseCase.InputData {}

  public record OutputData(@Valid PagedResult<ControlImplementation> page)
      implements UseCase.OutputData {}
}
