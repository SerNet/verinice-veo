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

import java.util.Date;

/**
 * Implements common fields and methods for objects in the entity layer.
 *
 * @author akoderman
 *
 */
public abstract class EntityLayerSupertype {

    public enum Lifecycle {
        CREATING, ACTIVE, STORING, LOADING, DELETING, DELETED, MODIFYING, ARCHIVING, ARCHIVED
    }

    private Key key;
    Unit unit;
    Lifecycle state;
    Date validFrom;
    Date validUntil;

    public Unit getUnit() {
        return unit;
    }

    protected void setUnit(Unit unit) {
        this.unit = unit;
    }

    protected EntityLayerSupertype(Key id, Lifecycle state, Date validFrom, Date validUntil) {
        this.key = id;
        this.state = state;
        this.validFrom = validFrom;
        this.validUntil = validUntil;
    }

    protected EntityLayerSupertype() {
    }

    public Key getKey() {
        return key;
    }

    protected void setKey(Key key) {
        this.key = key;
    }

}
