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
package org.veo.core.usecase.repository;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/*
* The repository provides access to business entities through a well-known interface.
*
* It provides methods to add, remove or reconstitute objects and encapsulates
* the actual interaction with the data store.
*
* The repository guarantees all invariants of the entities entity to maintain
* the integrity of the entities and all references. More than a simple data gateway,
* the repository does not just offer CRUD operations but
* uses factories and builders to build entities to specification.
* */
/**
 * @deprecated use {@link org.veo.core.repository.Repository}
 */
@Deprecated
@SuppressFBWarnings("NM_SAME_SIMPLE_NAME_AS_INTERFACE")
public interface Repository<T, K> extends org.veo.core.repository.Repository<T, K> {

}
