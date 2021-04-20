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
package org.veo.core.repository;

import java.util.List;
import java.util.Set;

import org.veo.core.entity.EntityLayerSupertype;
import org.veo.core.entity.Unit;

/**
 * A dynamic database query for retrieving {@link EntityLayerSupertype} objects.
 *
 * @param <T>
 *            Entity type
 */
public interface EntityLayerSupertypeQuery<T extends EntityLayerSupertype> {
    void whereUnitIn(Set<Unit> units);

    List<T> execute();

    void whereSubTypeIn(Set<String> values);
}
