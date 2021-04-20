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
package org.veo.core.usecase.repository;

import java.util.UUID;

import org.veo.core.entity.Domain;
import org.veo.core.entity.Key;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * A repository for <code>Domain</code> entities.
 *
 * Implements basic CRUD operations from the superinterface and extends them
 * with more specific methods - i.e. queries based on particular fields.
 *
 * @deprecated use {@link org.veo.core.repository.DomainRepository}
 */
@Deprecated
@SuppressFBWarnings("NM_SAME_SIMPLE_NAME_AS_INTERFACE")
public interface DomainRepository
        extends org.veo.core.repository.DomainRepository, Repository<Domain, Key<UUID>> {

}
