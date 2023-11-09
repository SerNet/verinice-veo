/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2019  Alexander Koderman.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.veo.core.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

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
@SuppressFBWarnings("PI_DO_NOT_REUSE_PUBLIC_IDENTIFIERS_CLASS_NAMES")
public interface Repository<T, K> {

  T save(T entity);

  List<T> saveAll(Set<T> entities);

  Optional<T> findById(K id);

  Set<T> findByIds(Set<K> ids);

  Set<T> getByIds(Set<K> ids);

  void delete(T entity);

  void deleteById(K id);

  boolean exists(K id);
}
