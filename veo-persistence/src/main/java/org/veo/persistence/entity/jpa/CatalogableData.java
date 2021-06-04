/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Urs Zeidler.
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

import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.hibernate.annotations.GenericGenerator;

import org.veo.core.entity.CatalogItem;
import org.veo.core.entity.Catalogable;
import org.veo.core.entity.Unit;
import org.veo.persistence.entity.jpa.validation.HasOwnerOrContainingCatalogItem;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity(name = "catalogable")
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@Data
@SuppressWarnings("PMD.AbstractClassWithoutAnyMethod")
@HasOwnerOrContainingCatalogItem
public abstract class CatalogableData extends BaseModelObjectData implements Catalogable {
    @Id
    @ToString.Include
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private String dbId;

    @NotNull
    @Column(name = "designator")
    @ToString.Include
    @Pattern(regexp = "([A-Z]{3}-\\d+)|NO_DESIGNATOR")
    private String designator;

    @ManyToMany(targetEntity = CatalogItemData.class, fetch = FetchType.LAZY)
    private Set<CatalogItem> appliedCatalogItems;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = UnitData.class)
    @JoinColumn(name = "owner_id")
    private Unit owner;

    @OneToOne(fetch = FetchType.LAZY, targetEntity = CatalogItemData.class)
    @JoinColumn(name = "containing_catalog_item_id")
    private CatalogItem containingCatalogItem;
}
