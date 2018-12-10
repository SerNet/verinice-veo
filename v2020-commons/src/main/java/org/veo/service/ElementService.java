/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.veo.service;

import java.util.List;

import org.veo.model.Element;

/**
 *
 * @author Daniel Murygin
 */
public interface ElementService {
    
    public Element save(Element element);
    
    public void delete(Element element);

    public Element load(String uuid);

    /**
     * Loads an element with references to other entities.
     * Properties, parent, children and links are loaded in an element
     * returned by this method.
     * 
     * @param uuid The id of an element
     * @return Element with given id and loaded references to all other entities
     */
    public Element loadWithAllReferences(String uuid);
    
    public List<Element> loadAll(String typeId);
    
    public Iterable<Element> findAll();
    
    public List<String> allRootElements();
}
