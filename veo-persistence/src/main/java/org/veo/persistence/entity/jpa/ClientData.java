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

import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.validation.Valid;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import org.veo.core.entity.Client;
import org.veo.core.entity.transform.TransformEntityToTargetContext;
import org.veo.core.entity.transform.TransformTargetToEntityContext;
import org.veo.persistence.entity.jpa.transformer.DataEntityToTargetContext;
import org.veo.persistence.entity.jpa.transformer.DataEntityToTargetTransformer;
import org.veo.persistence.entity.jpa.transformer.DataTargetToEntityContext;
import org.veo.persistence.entity.jpa.transformer.DataTargetToEntityTransformer;

@Entity(name = "client")
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
public class ClientData extends BaseModelObjectData {

    @Column(name = "name")
    @ToString.Include
    private String name;
    // many to one client-> unit
    @Column(name = "units")
    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<UnitData> units;
    // many to one client-> domain
    @Column(name = "domains")
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<DomainData> domains;

    public String getName() {
        return this.name;
    }

    public void setName(String aName) {
        this.name = aName;
    }

    public Set<UnitData> getUnits() {
        return this.units;
    }

    public void setUnits(Set<UnitData> aUnits) {
        this.units = aUnits;
    }

    public Set<DomainData> getDomains() {
        return this.domains;
    }

    public void setDomains(Set<DomainData> aDomains) {
        this.domains = aDomains;
    }

    /**
     * transform the given entity 'Client' to the corresponding 'ClientData' with
     * the DataEntityToTargetContext.getCompleteTransformationContext().
     */
    public static ClientData from(@Valid Client client) {
        return from(client, DataEntityToTargetContext.getCompleteTransformationContext());
    }

    /**
     * Transform the given data object 'ClientData' to the corresponding 'Client'
     * entity with the DataEntityToTargetContext.getCompleteTransformationContext().
     */
    public Client toClient() {
        return toClient(DataTargetToEntityContext.getCompleteTransformationContext());
    }

    public static ClientData from(@Valid Client client, TransformEntityToTargetContext tcontext) {
        if (tcontext instanceof DataEntityToTargetContext) {
            return DataEntityToTargetTransformer.transformClient2Data((DataEntityToTargetContext) tcontext,
                                                                      client);
        }
        throw new IllegalArgumentException("Wrong context type");
    }

    public Client toClient(TransformTargetToEntityContext tcontext) {
        if (tcontext instanceof DataTargetToEntityContext) {
            return DataTargetToEntityTransformer.transformData2Client((DataTargetToEntityContext) tcontext,
                                                                      this);
        }
        throw new IllegalArgumentException("Wrong context type");
    }

}
