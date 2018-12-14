/*******************************************************************************
 * Copyright (c) 2017 Daniel Murygin.
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

import org.veo.model.Element;

/**
 *
 * @author Daniel Murygin
 */
public interface ElementRepository extends CrudRepository<Element, String> {

    @Query(value = "select uuid from element e where e.parent_uuid is null", nativeQuery = true)
    public List<String> allRootElements();

    @Query("SELECT e FROM Element e left join fetch e.children where e.uuid = :uuid")
    @EntityGraph(value = "properties", type = EntityGraphType.LOAD)
    public Element findOneWithChildren(@Param("uuid") String uuid);

    @Query("SELECT e FROM Element e "
            + "LEFT JOIN FETCH e.linksOutgoing lEFT JOIN FETCH e.linksIncoming "
            + "WHERE e.uuid = :uuid")
    @EntityGraph(value = "linksWithProperties", type = EntityGraphType.LOAD)
    public Element findOneWithLinks(@Param("uuid") String uuid);

    @Query("SELECT DISTINCT e FROM Element e "
            + "LEFT JOIN FETCH e.properties LEFT JOIN FETCH e.children "
            + "LEFT JOIN FETCH e.parent LEFT JOIN FETCH e.linksOutgoing "
            + "LEFT JOIN FETCH e.linksIncoming WHERE e.uuid = :uuid")
    public Element findOneWithAll(@Param("uuid") String uuid);

    @Query("SELECT DISTINCT e FROM Element e left join fetch e.properties where e.typeId = :typeId")
    public List<Element> findByTypeId(@Param("typeId") String typeId);

    @Query("SELECT DISTINCT e FROM Element e left join fetch e.properties where e.parent.uuid = :uuidParent")
    public List<Element> findByParentId(@Param("uuidParent") String uuidParent);

    @EntityGraph(value = "properties", type = EntityGraphType.LOAD)
    public Element findByUuid(String id);

    @Query("SELECT DISTINCT e FROM Element e left join fetch e.properties")
    public Iterable<Element> findAllWithProperties();
}
