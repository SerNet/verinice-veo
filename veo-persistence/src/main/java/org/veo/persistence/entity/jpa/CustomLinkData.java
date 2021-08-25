/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2019  Urs Zeidler.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.veo.persistence.entity.jpa;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.validation.constraints.NotNull;

import org.veo.core.entity.CustomLink;
import org.veo.core.entity.Domain;
import org.veo.core.entity.EntityLayerSupertype;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity(name = "customlink")
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@Data
public class CustomLinkData extends CustomAspectData implements CustomLink {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY,
               targetEntity = EntityLayerSupertypeData.class,
               optional = true) // due to the single-table inheritance mapping, this must be
                                // nullable
    @JoinColumn(name = "target_id")
    @EqualsAndHashCode.Include
    private EntityLayerSupertype target;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY,
               targetEntity = EntityLayerSupertypeData.class,
               optional = true) // due to the single-table inheritance mapping, this must be
                                // nullable
    @JoinColumn(name = "source_id")
    @EqualsAndHashCode.Include
    private EntityLayerSupertype source;

    /**
     * Add the given Domain to the collection domains.
     *
     * @return true if added
     */
    public boolean addToDomains(Domain aDomain) {
        return this.domains.add(aDomain);
    }

    /**
     * Remove the given Domain from the collection domains.
     *
     * @return true if removed
     */
    public boolean removeFromDomains(Domain aDomain) {
        return this.domains.remove(aDomain);
    }

    public void setSource(EntityLayerSupertype aSource) {
        this.source = aSource;
    }
}
