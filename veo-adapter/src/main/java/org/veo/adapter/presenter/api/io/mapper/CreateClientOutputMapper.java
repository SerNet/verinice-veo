/*******************************************************************************
 * Copyright (c) 2019 Urs Zeidler.
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
package org.veo.adapter.presenter.api.io.mapper;

import java.util.Optional;

import org.veo.adapter.presenter.api.common.ApiResponseBody;
import org.veo.core.entity.Client;

/**
 * Returns just the id of the newly created Client as output.
 */
public final class CreateClientOutputMapper {

    public static ApiResponseBody map(Client client) {
        Optional<String> clientId = client.getId() == null ? Optional.empty()
                : Optional.ofNullable(client.getId()
                                            .uuidValue());
        ApiResponseBody apiResponseBody = new ApiResponseBody(true, clientId,
                "Client created successfully.");
        return apiResponseBody;
    }

}
