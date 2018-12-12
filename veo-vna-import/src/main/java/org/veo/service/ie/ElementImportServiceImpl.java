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
 *
 ******************************************************************************/
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
 * service layer in veo-core.
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
