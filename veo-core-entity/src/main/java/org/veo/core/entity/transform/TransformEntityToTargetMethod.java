/*******************************************************************************
 * Copyright (c) 2020 Urs Zeidler.
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
package org.veo.core.entity.transform;

import org.veo.core.entity.ModelObject;

/**
 * This interface defines the model transformer method from entity to target
 * (data/dto) as functional element.
 *
 * @param <S>
 *            source type an entity type
 * @param <T>
 *            target type a datat or dto type
 * @param <C>
 *            the transformcontex to use
 */
@FunctionalInterface
public interface TransformEntityToTargetMethod<S extends ModelObject, T, C extends TransformContext> {
    T map(C context, S source);
}