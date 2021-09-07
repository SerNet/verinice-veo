/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Jonas Jordan.
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
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.GenericGenerator;

import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;
import org.veo.core.entity.aspects.Aspect;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Entity(name = "aspect")
@Data
@SuppressWarnings("PMD.AbstractClassWithoutAnyMethod")
public abstract class AspectData implements Aspect {

    public AspectData(Domain domain, Element owner) {
        this.domain = domain;
        this.owner = owner;
    }

    protected AspectData() {
    }

    @Id
    @ToString.Include
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private String dbId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = DomainData.class, optional = false)
    @JoinColumn(name = "domain_id")
    @EqualsAndHashCode.Include
    private Domain domain;

    @EqualsAndHashCode.Include
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ElementData.class, optional = false)
    private Element owner;
}
