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
    long version;
    boolean isDraft;

    protected boolean isDraft() {
        return isDraft;
    }

    public void setDraft(boolean isDraft) {
        this.isDraft = isDraft;
    }

    public Unit getUnit() {
        return unit;
    }

    public void setUnit(Unit unit) {
        this.unit = unit;
    }

    protected EntityLayerSupertype(Key id, Lifecycle state, Date validFrom, Date validUntil, long version, boolean isDraft) {
        this.key = id;
        this.state = state;
        this.validFrom = validFrom;
        this.validUntil = validUntil;
        this.version = version;
        this.isDraft = isDraft;
    }
    
    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public EntityLayerSupertype() {
    }

    public Key getKey() {
        return key;
    }

    public Lifecycle getState() {
        return state;
    }

    public void setState(Lifecycle state) {
        this.state = state;
    }

    public Date getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(Date validFrom) {
        this.validFrom = validFrom;
    }

    public Date getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(Date validUntil) {
        this.validUntil = validUntil;
    }

    public void setKey(Key key) {
        this.key = key;
    }

}
