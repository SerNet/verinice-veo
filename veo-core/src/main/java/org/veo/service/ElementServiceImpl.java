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
 ******************************************************************************/
package org.veo.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.veo.model.Element;
import org.veo.persistence.ElementRepository;

/**
 *
 * @author Daniel Murygin
 */
@Service
public class ElementServiceImpl implements ElementService {

    @Autowired
    ElementRepository elementRepository;

    @Override
    public Element save(Element element) {
        return elementRepository.save(element);
    }

    @Override
    public Element load(String uuid) {
        return elementRepository.findByUuid(uuid);
    }

    @Override
    public Element loadWithAllReferences(String uuid) {
        return elementRepository.findOneWithAll(uuid);
    }

    @Override
    public List<Element> loadAll(String typeId) {
        return elementRepository.findByTypeId(typeId);
    }

    public Iterable<Element> findAll() {
        return elementRepository.findAll();
    }

    public List<String> allRootElements() {
        return elementRepository.allRootElements();
    }

    @Override
    public void delete(Element element) {
        elementRepository.delete(element);
    }

}
