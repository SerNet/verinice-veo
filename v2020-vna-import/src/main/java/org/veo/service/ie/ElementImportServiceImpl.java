/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.veo.service.ie;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.veo.model.Element;
import org.veo.service.ElementService;
import org.veo.service.LinkService;

/**
 * A service facade providing all methods to import data from a VNA to a
 * database. This implementation delegates calls to the services from the
 * service layer in v2020-service.
 * 
 * @author Daniel Murygin
 */
@Service
public class ElementImportServiceImpl implements ImportElementService {

    @Autowired
    ElementService elementService;

    @Autowired
    LinkService linkService;

    @Override
    public void createLink(String sourceId, List<String> destinationIdList, String type) {
        for (String destinationId : destinationIdList) {
            linkService.save(sourceId, destinationId);
        }
    }

    @Override
    public void create(Element element) {
        elementService.save(element);
    }

}
