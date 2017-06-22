/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.veo.ie;

import java.util.List;
import org.veo.model.Element;

/**
 *
 * @author Daniel Murygin
 */
public interface ImportElementService {

    public void createLink(String sourceId, List<String> destinationIdList, String type);

    public void create(Element element);
    
}
