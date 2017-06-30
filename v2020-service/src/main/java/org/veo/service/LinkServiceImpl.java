/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.veo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.veo.model.Element;
import org.veo.model.Link;
import org.veo.persistence.ElementRepository;
import org.veo.persistence.LinkRepository;

/**
 *
 * @author Daniel Murygin
 */
@Service
public class LinkServiceImpl implements LinkService {

    @Autowired
    LinkRepository linkRepository;
    
    @Autowired
    ElementRepository elementRepository;
    
    @Override
    public Link save(Element source, Element destination) {
        Link link = new Link();
        link.setSource(source);
        link.setDestination(destination);
        return save(link);
    }

    @Override
    public Link save(String sourceId, String destinationId) {
        Element source = elementRepository.findOne(sourceId);
        Element destination = null;
        if(sourceId.equals(destinationId)) {
            destination = source;
        } else {
            destination = elementRepository.findOne(destinationId);
        }
        return save(source, destination);
    }

    @Override
    public Link save(Link link) {
       return  linkRepository.save(link);
    }
    
    @Override
    public Iterable<Link> getAll(){
        return linkRepository.findAll();
    }
    
}
