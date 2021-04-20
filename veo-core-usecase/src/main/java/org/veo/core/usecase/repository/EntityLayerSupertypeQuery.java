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
package org.veo.core.usecase.repository;

import org.veo.core.entity.EntityLayerSupertype;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * A dynamic database query for retrieving {@link EntityLayerSupertype} objects.
 *
 * @param <T>
 *            Entity type
 * @deprecated use {@link org.veo.core.repository.EntityLayerSupertypeQuery}
 */
@Deprecated
@SuppressFBWarnings("NM_SAME_SIMPLE_NAME_AS_INTERFACE")
public interface EntityLayerSupertypeQuery<T extends EntityLayerSupertype>
        extends org.veo.core.repository.EntityLayerSupertypeQuery<T> {

}
