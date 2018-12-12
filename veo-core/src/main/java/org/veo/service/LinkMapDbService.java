/*******************************************************************************
 * Copyright (c) 2018 Urs Zeidler.
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
 * Contributors:
 *     Urs Zeidler uz[at]sernet.de - initial API and implementation
 ******************************************************************************/
package org.veo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.veo.commons.VeoException;
import org.veo.model.Link;
import org.veo.persistence.LinkRepository;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * The database backed service to persist link.
 */
@Service("LinkDatabaseService") @Lazy public class LinkMapDbService implements LinkMapService {
    @Autowired LinkRepository elementRepository;

    @Autowired JsonFactory jsonFactory;

    @Autowired LinkFactory elementFactory;

    @Override public List<Map<String, Object>> findAll() {
        Iterable<Link> allElements = elementRepository.findAll();
        return getResultList(allElements);
    }

    @Override public Map<String, Object> find(String id) {
        if (!elementRepository.existsById(id)) {
            throw new VeoException(VeoException.Error.ELEMENT_NOT_FOUND,
                    "Element with uuid %id% does not exists.", "id", id);
        }
        Map<String, Object> result = null;
        Link link = elementRepository.findByUuid(id);
        if (link != null) {
            result = jsonFactory.createJson(link);
        }
        return result;
    }

    @Override public List<Map<String, Object>> findByElement(String elementId) {
        List<Link> linksList = elementRepository.findBySourceOrTarget(elementId);
        return getResultList(linksList);
    }

    @Override public void save(String id, Map<String, Object> json) {
        Link link = elementRepository.findByUuid(id);
        if (link == null) {
            throw new VeoException(VeoException.Error.ELEMENT_NOT_FOUND,
                    "Element with uuid %id% does not exists.", "id", id);
        }
        json.put(JsonFactory.ID, id);
        link = elementFactory.updateLink(json, link);
        elementRepository.save(link);
    }

    @Override public String saveNew(Map<String, Object> json) {
        Link link = elementFactory.createLink(json);
        String uuid = link.getUuid();
        if (uuid != null && elementRepository.existsById(uuid)) {
            throw new VeoException(VeoException.Error.ELEMENT_EXISTS,
                    "Element with uuid " + uuid + " already exists.");
        }
        return elementRepository.save(link).getUuid();
    }

    @Override public void delete(String id) {
        elementRepository.deleteById(id);
    }

    private List<Map<String, Object>> getResultList(Iterable<Link> allElements) {
        List<Map<String, Object>> result = new LinkedList<>();
        for (Link element : allElements) {
            result.add(jsonFactory.createJson(element));
        }
        return result;
    }
}
