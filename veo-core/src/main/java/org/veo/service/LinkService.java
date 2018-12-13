/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.veo.service;

import org.veo.model.Element;
import org.veo.model.Link;

/**
 *
 * @author Daniel Murygin
 */
public interface LinkService {

    Link save(Element source, Element destination);

    Link save(String sourceId, String destinationId);

    Link save(Link link);

    Iterable<Link> getAll();
}
