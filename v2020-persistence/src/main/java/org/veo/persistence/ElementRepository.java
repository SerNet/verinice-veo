/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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

    @Query("SELECT e FROM Element e " + 
           "LEFT JOIN FETCH e.linksOutgoing " +
           "lEFT JOIN FETCH e.linksIncoming " + 
           "WHERE e.uuid = :uuid")
    @EntityGraph(value = "linksWithProperties", type = EntityGraphType.LOAD)
    public Element findOneWithLinks(@Param("uuid") String uuid);
    
    @Query("SELECT DISTINCT e FROM Element e " + 
            "LEFT JOIN FETCH e.properties " +
            "LEFT JOIN FETCH e.children " +
            "LEFT JOIN FETCH e.parent " +
            "LEFT JOIN FETCH e.linksOutgoing " +
            "LEFT JOIN FETCH e.linksIncoming " + 
            "WHERE e.uuid = :uuid")
    public Element findOneWithAll(@Param("uuid") String uuid);

    @Query("SELECT DISTINCT e FROM Element e left join fetch e.properties where e.typeId = :typeId")
    public List<Element> findByTypeId(@Param("typeId") String typeId);
    
    @EntityGraph(value = "properties", type = EntityGraphType.LOAD)
    public Element findOne(String id);
    
    @Query("SELECT DISTINCT e FROM Element e left join fetch e.properties")
    public Iterable<Element> findAllWithProperties();
}
