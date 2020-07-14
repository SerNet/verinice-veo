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
package org.veo.adapter.presenter.api.process;

import org.veo.core.entity.Client;
import org.veo.core.entity.EntityLayerSupertype;
import org.veo.core.entity.Key;
import org.veo.core.usecase.base.CreateEntityInputData;

/**
 * Map between the request DTO received from a client and the input expected by
 * the data source. (This is not needed for simple input data.)
 *
 * The request DTO is not mapped to an entity in this case because a new entity
 * is created from the input values.
 */
public final class CreateEntityInputMapper {

    public static <T extends EntityLayerSupertype> CreateEntityInputData<T> map(Client client,
            String unitId, T entity) {
        return new CreateEntityInputData<>(Key.uuidFrom(unitId), entity, client);
    }

    private CreateEntityInputMapper() {
    }

}
