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
package org.veo.core.entity;

import java.util.List;
import java.util.Optional;

/*
* The repository provides access to entities through a well-known interface.
*
* It provides methods to add, remove or reconstitute objects and encapsulates
* the actual interaction with the data store.
*
* The repository guarantees all invariants of the entities entity to maintain
* the integrity of the entities and all references. More than a simple data gateway,
* the repository does not just offer CRUD operations but
* uses factories and builders to build entities to specification.
* */
public interface IRepository<T, U> {

    public T save(T entity);

    public Optional<T> findById(Key<U> id);
    
    public List<T> findByName(String search);
    
    public void delete(T entity);

    public void deleteById(Key<U> entity);
    

}
