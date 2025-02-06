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
package org.veo.adapter.presenter.api.unit;

import java.util.UUID;

import org.veo.adapter.presenter.api.io.mapper.PagingMapper;
import org.veo.core.entity.Client;
import org.veo.core.entity.Control;
import org.veo.core.entity.RiskAffected;
import org.veo.core.entity.ref.TypedId;
import org.veo.core.usecase.compliance.GetRequirementImplementationsByControlImplementationUseCase;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class GetRequirementImplementationsByControlImplementationInputMapper {
  public static GetRequirementImplementationsByControlImplementationUseCase.InputData map(
      Client authenticatedClient,
      Class<? extends RiskAffected<?, ?>> clazz,
      UUID riskAffectedId,
      UUID controlId,
      Integer pageSize,
      Integer pageNumber,
      String sortColumn,
      String sortOrder) {
    return new GetRequirementImplementationsByControlImplementationUseCase.InputData(
        authenticatedClient,
        TypedId.from(riskAffectedId, clazz),
        TypedId.from(controlId, Control.class),
        PagingMapper.toConfig(pageSize, pageNumber, sortColumn, sortOrder));
  }
}
