/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Urs Zeidler.
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
package org.veo.persistence.access.jpa;

import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import org.veo.persistence.entity.jpa.DomainData;

public interface DomainDataRepository extends CrudRepository<DomainData, String> {

    @Query("select e from #{#entityName} as e join e.catalogs as c join c.catalogItems as i where i.dbId = ?1")
    Optional<DomainData> findByCatalogsCatalogItemsId(String catalogItemId);

    @Query("select d from #{#entityName} d left join fetch d.elementTypeDefinitions where d.owner.id = ?1")
    Set<DomainData> findAllByClient(String clientId);
}
