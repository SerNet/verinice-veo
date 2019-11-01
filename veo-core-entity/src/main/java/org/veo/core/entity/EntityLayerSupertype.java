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

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Past;
import javax.validation.constraints.PastOrPresent;
import javax.validation.constraints.PositiveOrZero;

/**
 * Implements common fields and methods for objects in the entity layer.
 *
 * @author akoderman
 *
 */
public abstract class EntityLayerSupertype {

    /**
     * 
     *
     */
    public enum Lifecycle {
        CREATING,
        DRAFT,
        ACTIVE,
        DELETED,
        ARCHIVED
    }

    @NotNull
    private Key<UUID> key;
    
    @NotNull
    Unit unit;
    
    @NotNull
    Lifecycle state;

    @PastOrPresent(message="The start of the entity's validity must be in the past.")
    @NotNull(message="The start of the entity's validity must be in the past.")
    Instant validFrom;
    
    
    @PastOrPresent(message="The end of the entity's validity must be be set in the past or set to 'null' if it is currently still valid.")
    Instant validUntil;

    @PositiveOrZero
    long version;

    public Unit getUnit() {
        return unit;
    }

    public void setUnit(Unit unit) {
        this.unit = unit;
    }

    protected EntityLayerSupertype(Key<UUID> id, Lifecycle state, Instant validFrom, Instant validUntil, long version, boolean isDraft) {
        this.key = id;
        this.state = state;
        this.validFrom = validFrom;
        this.validUntil = validUntil;
        this.version = version;
    }
    
    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public EntityLayerSupertype() {
    }

    public Key<UUID> getKey() {
        return key;
    }

    public Lifecycle getState() {
        return state;
    }

    public void setState(Lifecycle state) {
        this.state = state;
    }

    public Instant getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(Instant validFrom) {
        this.validFrom = validFrom;
    }

    public Instant getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(Instant validUntil) {
        this.validUntil = validUntil;
    }

    public void setKey(Key<UUID> key) {
        this.key = key;
    }

}
