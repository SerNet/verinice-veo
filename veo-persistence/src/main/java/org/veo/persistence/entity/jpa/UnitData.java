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
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import org.veo.core.entity.Unit;
import org.veo.core.entity.transform.TransformEntityToTargetContext;
import org.veo.core.entity.transform.TransformTargetToEntityContext;
import org.veo.persistence.entity.jpa.transformer.DataEntityToTargetContext;
import org.veo.persistence.entity.jpa.transformer.DataEntityToTargetTransformer;
import org.veo.persistence.entity.jpa.transformer.DataTargetToEntityContext;
import org.veo.persistence.entity.jpa.transformer.DataTargetToEntityTransformer;

@Entity(name = "unit")
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
public class UnitData extends BaseModelObjectData implements NameAbleData {

    @NotNull
    @Column(name = "name")
    @ToString.Include
    private String name;
    @Column(name = "abbreviation")
    private String abbreviation;
    @Column(name = "description")
    private String description;
    @NotNull
    // one to one unit-> client
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private ClientData client;
    // many to one unit-> unit
    @Column(name = "units")
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<UnitData> units;
    // one to one unit-> unit
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private UnitData parent;
    // many to one unit-> domain
    @Column(name = "domains")
    @ManyToMany
    private Set<DomainData> domains;

    public String getName() {
        return this.name;
    }

    public void setName(String aName) {
        this.name = aName;
    }

    public String getAbbreviation() {
        return this.abbreviation;
    }

    public void setAbbreviation(String aAbbreviation) {
        this.abbreviation = aAbbreviation;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String aDescription) {
        this.description = aDescription;
    }

    public ClientData getClient() {
        return this.client;
    }

    public void setClient(ClientData aClient) {
        this.client = aClient;
    }

    public Set<UnitData> getUnits() {
        return this.units;
    }

    public void setUnits(Set<UnitData> aUnits) {
        this.units = aUnits;
    }

    public UnitData getParent() {
        return this.parent;
    }

    public void setParent(UnitData aParent) {
        this.parent = aParent;
    }

    public Set<DomainData> getDomains() {
        return this.domains;
    }

    public void setDomains(Set<DomainData> aDomains) {
        this.domains = aDomains;
    }

    /**
     * transform the given entity 'Unit' to the corresponding 'UnitData' with the
     * DataEntityToTargetContext.getCompleteTransformationContext().
     */
    public static UnitData from(@Valid Unit unit) {
        return from(unit, DataEntityToTargetContext.getCompleteTransformationContext());
    }

    /**
     * Transform the given data object 'UnitData' to the corresponding 'Unit' entity
     * with the DataEntityToTargetContext.getCompleteTransformationContext().
     */
    public Unit toUnit() {
        return toUnit(DataTargetToEntityContext.getCompleteTransformationContext());
    }

    public static UnitData from(@Valid Unit unit, TransformEntityToTargetContext tcontext) {
        if (tcontext instanceof DataEntityToTargetContext) {
            return DataEntityToTargetTransformer.transformUnit2Data((DataEntityToTargetContext) tcontext,
                                                                    unit);
        }
        throw new IllegalArgumentException("Wrong context type");
    }

    public Unit toUnit(TransformTargetToEntityContext tcontext) {
        if (tcontext instanceof DataTargetToEntityContext) {
            return DataTargetToEntityTransformer.transformData2Unit((DataTargetToEntityContext) tcontext,
                                                                    this);
        }
        throw new IllegalArgumentException("Wrong context type");
    }

}
