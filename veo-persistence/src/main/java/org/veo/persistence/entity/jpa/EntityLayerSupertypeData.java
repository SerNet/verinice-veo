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
 *
 * Contributors:
 *     Alexander Koderman <ak@sernet.de> - initial API and implementation
 ******************************************************************************/
package org.veo.persistence.entity.jpa;

import java.time.Instant;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.hibernate.classic.Lifecycle;
import org.modelmapper.ModelMapper;
import org.veo.core.entity.EntityLayerSupertype;
import org.veo.core.entity.Key;

@Entity
@Inheritance(strategy=InheritanceType.JOINED)
@IdClass(Key.class)
public abstract class EntityLayerSupertypeData {

    @Id
    private Key<UUID> uuid;
    
    @ManyToOne
    @JoinColumn(name="unit_id", nullable=false)
    private UnitData unit;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable=false)
    Lifecycle state;
    
    @Column(name="valid_from", nullable=false)
    private Instant validFrom;
    
    @Column(name="valid_until", nullable=true)
    private Instant validUntil;

    
    
    public Key<UUID> getUuid() {
        return uuid;
    }

    public void setUuid(Key<UUID> uuid) {
        this.uuid = uuid;
    }

    public UnitData getUnit() {
        return unit;
    }

    public void setUnit(UnitData unit) {
        this.unit = unit;
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
    
    
    
    
}
