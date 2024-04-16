/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2024  Jonas Jordan
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
package org.veo.core.usecase;

import java.util.Map;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

import org.veo.core.entity.Action;
import org.veo.core.entity.Element;
import org.veo.core.entity.Key;
import org.veo.core.repository.DomainRepository;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class GetAvailableActionsUseCase
    implements TransactionalUseCase<
        GetAvailableActionsUseCase.InputData, GetAvailableActionsUseCase.OutputData> {
  private final DomainRepository domainRepository;

  @Override
  public OutputData execute(InputData input) {
    var domain = domainRepository.getActiveById(input.domainId, input.clientId);
    return new OutputData(domain.getAvailableActions(input.elementType));
  }

  public record InputData(
      @NotNull Key<UUID> domainId,
      @NotNull Key<UUID> elementId,
      @NotNull Class<? extends Element> elementType,
      @NotNull Key<UUID> clientId)
      implements UseCase.InputData {}

  public record OutputData(@NotNull Map<String, Action> actions) implements UseCase.OutputData {}
}
