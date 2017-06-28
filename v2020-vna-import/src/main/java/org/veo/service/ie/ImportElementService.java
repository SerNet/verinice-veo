/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.veo.service.ie;

import java.util.List;
import org.veo.model.Element;

/**
 * A service facade providing all methods to import data from a VNA to a
 * database.
 *
 * @author Daniel Murygin
 */
public interface ImportElementService {

    /**
     * Creates links of a given type from the element with UUID sourceId to all
     * elements with the UUIDs in destinationIdList.
     * 
     * @param sourceId
     *            The UUID of the source element
     * @param destinationIdList
     *            A list with UUIDs of the destination elements.
     * @param type
     *            The type (id) of the links.
     */
    public void createLink(String sourceId, List<String> destinationIdList, String type);

    /**
     * Creates an element
     * 
     * @param element
     */
    public void create(Element element);

}
