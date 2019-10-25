/*******************************************************************************
 * Copyright (c) 2019 Alexander Koderman.
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
package org.veo.core.entity;

import java.util.Collection;

public class Unit {

    private Collection<EntityLayerSupertype> elements;
    private Collection<Unit> units;
    private Client client;

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public Collection<Unit> getUnits() {
        return units;
    }

    public void setUnits(Collection<Unit> units) {
        this.units = units;
    }

    public Collection<EntityLayerSupertype> getElements() {
        return elements;
    }

    public void setElements(Collection<EntityLayerSupertype> elements) {
        this.elements = elements;
    }

    public void addElement(EntityLayerSupertype element) {
        elements.add(element);
    }

}
