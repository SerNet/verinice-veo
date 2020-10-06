/*******************************************************************************
 * Copyright (c) 2019 Alexander Koderman.
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.veo.adapter.presenter.api.unit;

import java.util.Optional;
import java.util.UUID;

import org.veo.adapter.presenter.api.common.ModelObjectReference;
import org.veo.adapter.presenter.api.dto.create.CreateUnitDto;
import org.veo.core.entity.Key;
import org.veo.core.usecase.common.NameableInputData;
import org.veo.core.usecase.unit.CreateUnitUseCase;
import org.veo.core.usecase.unit.CreateUnitUseCase.InputData;

/**
 * Map between the request DTO received from a client and the input expected by
 * the data source. (This is not needed for simple input data. In these cases
 * the constructor of InputData can be called directly.)
 *
 * The request DTO is not mapped to a unit object in this case because a new
 * unit is created from the input values. All values from the DTO that are not
 * relevant for this operation are ignored. Illegal values should cause an
 * IllegalArgumentException.
 *
 * If the client provided a UUID it is used to create the new unit. If not, a
 * new one is generated.
 *
 * The newly created unit is then returned by the use case:
 *
 * @see CreateUnitOutputMapper
 */
public final class CreateUnitInputMapper {

    public static CreateUnitUseCase.InputData map(CreateUnitDto dto, String clientId) {
        Optional<Key<UUID>> parentId = Optional.ofNullable(dto.getParent())
                                               .map(ModelObjectReference::getId)
                                               .map(Key::uuidFrom);

        Optional<Key<UUID>> newUnitId = Optional.ofNullable(dto.getId())
                                                .map(Key::uuidFrom);

        NameableInputData namedInput = new NameableInputData();
        namedInput.setId(newUnitId);
        namedInput.setName(dto.getName());
        namedInput.setAbbreviation(dto.getAbbreviation());
        namedInput.setDescription(dto.getDescription());

        return new InputData(namedInput, Key.uuidFrom(clientId), parentId);
    }

}
