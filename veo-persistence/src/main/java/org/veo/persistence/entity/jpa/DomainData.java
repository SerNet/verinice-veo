/*******************************************************************************
 * Copyright (c) 2019 Urs Zeidler.
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
package org.veo.persistence.entity.jpa;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.validation.constraints.NotNull;

import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity(name = "domain")
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@Data
public class DomainData extends BaseModelObjectData implements NameableData, Domain {

    @NotNull
    @ToString.Include
    private String name;

    private String abbreviation;

    private String description;

    private Boolean active;

    // This enforces the composition association Client-Domain
    @ManyToOne(targetEntity = ClientData.class, optional = false, fetch = FetchType.LAZY)
    @NotNull
    private Client owner;

    @Override
    public Boolean isActive() {
        return active;
    }

}
