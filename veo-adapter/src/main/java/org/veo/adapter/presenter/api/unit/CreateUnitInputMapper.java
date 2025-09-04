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
package org.veo.adapter.presenter.api.unit;

import java.util.stream.Collectors;

import org.veo.adapter.presenter.api.common.IdRef;
import org.veo.adapter.presenter.api.dto.create.CreateUnitDto;
import org.veo.adapter.presenter.api.io.mapper.CreateOutputMapper;
import org.veo.core.usecase.common.NameableInputData;
import org.veo.core.usecase.unit.CreateUnitUseCase;
import org.veo.core.usecase.unit.CreateUnitUseCase.InputData;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Map between the request DTO received from a client and the input expected by the data source.
 * (This is not needed for simple input data. In these cases the constructor of InputData can be
 * called directly.)
 *
 * <p>The request DTO is not mapped to a unit object in this case because a new unit is created from
 * the input values. All values from the DTO that are not relevant for this operation are ignored.
 * Illegal values should cause an IllegalArgumentException.
 *
 * <p>If the client provided a UUID it is used to create the new unit. If not, a new one is
 * generated.
 *
 * <p>The newly created unit is then returned by the use case:
 *
 * @see CreateOutputMapper
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CreateUnitInputMapper {

  public static CreateUnitUseCase.InputData map(CreateUnitDto dto) {

    return new InputData(
        new NameableInputData(dto.getName(), dto.getAbbreviation(), dto.getDescription()),
        dto.getDomains().stream().map(IdRef::getId).collect(Collectors.toSet()));
  }
}
