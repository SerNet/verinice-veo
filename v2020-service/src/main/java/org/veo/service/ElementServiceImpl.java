/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.veo.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.veo.model.Element;
import org.veo.persistence.ElementRepository;

/**
 *
 * @author Daniel Murygin
 */
@Service
public class ElementServiceImpl implements ElementService {

    @Autowired
    ElementRepository elementRepository;
    
    @Override
    public Element save(Element element) {
        return elementRepository.save(element);
    }
    
    @Override
    public Element load(String uuid) {
        return elementRepository.findOne(uuid);
    }
    
    @Override
    public Element loadWithAllReferences(String uuid) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<Element> loadAll(String typeId) {
        return elementRepository.findByTypeId(typeId);
    }

    
    
}
