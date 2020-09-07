/*******************************************************************************
 * Copyright (c) 2020 Jonas Jordan.
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
package org.veo.adapter.presenter.api.response.transformer;

import org.veo.core.entity.transform.EntityFactory;

/**
 * Creates {@link DtoToEntityContext} instances.
 */
public class DtoToEntityContextFactory {
    private final EntityFactory entityFactory;

    public DtoToEntityContextFactory(EntityFactory entityFactory) {
        this.entityFactory = entityFactory;
    }

    public DtoToEntityContext create() {
        return new DtoToEntityContext(entityFactory);
    }
}
