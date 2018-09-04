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
package org.veo.service;

import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class DummyLinkMapService implements LinkMapService {
    @Override public List<Map<String, Object>> findAll() {
        return Collections.emptyList();
    }

    @Override public Map<String, Object> find(String id) {
        return new HashMap<>();
    }

    @Override public List<Map<String, Object>> findByElement(String elementId) {
        return Collections.emptyList();
    }

    @Override public void save(String id, Map<String, Object> content) {

    }

    @Override public String saveNew(Map<String, Object> content) {
        return UUID.randomUUID().toString();
    }

    @Override public void delete(String id) {

    }
}
