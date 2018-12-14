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

import org.veo.model.Element;

/**
 *
 * @author Daniel Murygin
 */
public interface ElementService {

    public Element save(Element element);

    public void delete(Element element);

    public Element load(String uuid);

    /**
     * Loads an element with references to other entities. Properties, parent,
     * children and links are loaded in an element returned by this method.
     *
     * @param uuid
     *            The id of an element
     * @return Element with given id and loaded references to all other entities
     */
    public Element loadWithAllReferences(String uuid);

    public List<Element> loadAll(String typeId);

    public Iterable<Element> findAll();

    public List<String> allRootElements();
}
