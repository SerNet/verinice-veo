/*******************************************************************************
 * Copyright (c) 2021 Urs Zeidler.
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

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.veo.core.entity.Catalog;
import org.veo.core.entity.CatalogItem;
import org.veo.core.entity.Catalogable;
import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.core.entity.ElementOwner;
import org.veo.core.entity.TailoringReference;
import org.veo.core.entity.UpdateReference;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity(name = "catalogitem")
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@Data
public class CatalogItemData extends ElementOwnerData implements CatalogItem, ElementOwner {

    @ManyToOne(targetEntity = CatalogData.class)
    private Catalog catalog;

    @Column(name = "tailoringreference")
    @OneToMany(cascade = CascadeType.ALL,
               orphanRemoval = true,
               targetEntity = TailoringReferenceData.class,
               mappedBy = "owner",
               fetch = FetchType.LAZY)
    private Set<TailoringReference> tailoringReference = new HashSet<>();

    @ManyToOne(optional = true, targetEntity = CatalogableData.class, cascade = CascadeType.ALL)
    private Catalogable element;

    @Column(name = "updatereferences")
    @OneToMany(cascade = CascadeType.ALL,
               orphanRemoval = true,
               targetEntity = UpdateReferenceData.class,
               mappedBy = "owner",
               fetch = FetchType.LAZY)
    private Set<UpdateReference> updateReferences = new HashSet<>();

    @ToString.Include
    @Column(name = "namespace")
    private String namespace;

    @Override
    public Client getClient() {
        if (getCatalog().getDomainTemplate()
                        .getModelInterface()
                        .isAssignableFrom(Domain.class))
            return ((Domain) getCatalog().getDomainTemplate()).getOwner();
        else
            return null;
    }
}
