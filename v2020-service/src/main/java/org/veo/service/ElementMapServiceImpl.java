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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.veo.persistence.JsonFileRepository;
import org.veo.persistence.MapRepository;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implements ElementMapService storing maps as JSON on the file system.
 */
@Service
public class ElementMapServiceImpl implements ElementMapService {

    private MapRepository mapRepository;

    @Autowired
    public ElementMapServiceImpl(VeoConfigurationService configuration) throws IOException {
        this(new JsonFileRepository(new File(configuration.getBaseDir(), "elements")));
    }

    public ElementMapServiceImpl(MapRepository mapRepository) {
        this.mapRepository = mapRepository;
    }

    @Override
    public List<Map<String, Object>> findAll() throws IOException {
        return mapRepository.readAll();
    }

    @Override
    public Map<String, Object> find(String id) throws IOException {
        return mapRepository.read(id);
    }

    @Override
    public List<Map<String, Object>> findChildren(String id) throws IOException {
        List<Map<String, Object>> elements =  mapRepository.readAll();
        return elements.stream()
                .filter(m -> {
                    Map<String, String> parent = ((Map<String, String>)m.get("parent"));
                    return parent != null && parent.get("$ref").equals("/elements/" + id);
                })
                .collect(Collectors.toList());
    }

    @Override
    public void save(String id, Map<String, Object> content) throws IOException {
        mapRepository.update(id, content);
    }

    @Override
    public String saveNew(Map<String, Object> content) throws IOException {
        return mapRepository.create(content);
    }

    @Override
    public void delete(String id) throws IOException {
        this.mapRepository.delete(id);
    }
}
