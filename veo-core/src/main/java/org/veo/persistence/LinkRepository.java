/*******************************************************************************
 * Copyright (c) 2018 Jochen Kemnade.
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
package org.veo.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.EntityGraph.EntityGraphType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import org.veo.model.Link;

/**
 *
 * @author Daniel Murygin
 */
public interface LinkRepository extends CrudRepository<Link, String> {

    @EntityGraph(value = "properties", type = EntityGraphType.LOAD)
    public Link findByUuid(String id);

    @Query("SELECT DISTINCT l from Link l LEFT JOIN FETCH l.properties "
            + "where l.source.uuid = :uuid or l.destination.uuid = :uuid")
    public List<Link> findBySourceOrTarget(@Param("uuid") String elementId);

}
