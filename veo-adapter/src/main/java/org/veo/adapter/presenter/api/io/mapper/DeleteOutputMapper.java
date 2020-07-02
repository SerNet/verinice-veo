/*******************************************************************************
 * Copyright (c) 2020 Alexander Koderman.
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
import org.veo.core.entity.Key;
import org.veo.core.entity.ModelObject;

public class DeleteOutputMapper {

    public static ApiResponseBody map(ModelObject object) {
        Optional<String> id = Optional.ofNullable(object.getId())
                                      .map(Key::uuidValue);
        return new ApiResponseBody(true, id, "Object deleted successfully.");
    }
}
