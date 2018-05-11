/*******************************************************************************
 * Copyright (c) 2018 Alexander Ben Nasrallah.
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
 *     Alexander Ben Nasrallah <an@sernet.de> - initial API and implementation
 ******************************************************************************/
package org.veo.service

import org.veo.persistence.MapRepository;

public class MockRepo implements MapRepository {
    private def elements
    private String nextId;

    public MockRepo(elements) {
        this.elements = elements
        this.nextId = "1"
    }

    @Override
    List<Map<String, Object>> readAll() throws IOException {
        return elements
    }

    @Override
    Map<String, Object> read(String id) throws IOException {
        return elements.find { it['id'] == id }
    }

    @Override
    void update(String id, Map<String, Object> content) throws IOException {
        def toRemove = elements.find { it['id'] == id }
        elements.remove(toRemove)
        content['id'] = id
        elements.add(content)
    }

    @Override
    String create(Map<String, Object> content) throws IOException {
        content.put("id", nextId)
        elements.add(content)
        return nextId
    }

    @Override
    void delete(String id) throws IOException {
        def toRemove = elements.find { it['id'] == id }
        elements.remove(toRemove)
    }

    void setNextId(String id) {
        this.nextId = id
    }
}
