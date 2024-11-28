/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Jonas Jordan
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
package org.veo.adapter.presenter.api.io.mapper;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.veo.core.entity.Client;
import org.veo.core.entity.Element;
import org.veo.core.entity.state.ElementState;
import org.veo.core.usecase.base.CreateElementUseCase;

public class CreateElementInputMapper {
  /** Creates input data for element creation. */
  public static <T extends Element> CreateElementUseCase.InputData<T> map(
      ElementState<T> state, Client client, List<UUID> scopeIds) {
    return new CreateElementUseCase.InputData<>(state, client, mapIds(scopeIds));
  }

  private static Set<UUID> mapIds(List<UUID> ids) {
    if (ids == null) {
      return Set.of();
    }
    return new HashSet<>(ids);
  }
}
