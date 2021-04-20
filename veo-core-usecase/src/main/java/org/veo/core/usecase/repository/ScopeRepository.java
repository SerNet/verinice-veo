/*******************************************************************************
 * Copyright (c) 2021 Jochen Kemnade.
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

import org.veo.core.entity.Scope;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * A repository for <code>Scope</code> entities.
 *
 * Implements basic CRUD operations from the superinterface and extends them
 * with more specific methods - i.e. queries based on particular fields.
 *
 * @deprecated use {@link org.veo.core.repository.ScopeRepository}
 */
@Deprecated
@SuppressFBWarnings("NM_SAME_SIMPLE_NAME_AS_INTERFACE")
public interface ScopeRepository
        extends org.veo.core.repository.ScopeRepository, EntityLayerSupertypeRepository<Scope> {

}
