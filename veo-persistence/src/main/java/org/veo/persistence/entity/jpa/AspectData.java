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

import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;

import org.hibernate.annotations.GenericGenerator;

import org.veo.core.entity.DomainBase;
import org.veo.core.entity.Element;
import org.veo.core.entity.aspects.Aspect;

import lombok.Data;
import lombok.ToString;

@MappedSuperclass
@ToString(onlyExplicitlyIncluded = true)
@Data
@SuppressWarnings("PMD.AbstractClassWithoutAnyMethod")
public abstract class AspectData implements Aspect {

  public AspectData(DomainBase domain, Element owner) {
    this.domain = domain;
    this.owner = owner;
  }

  protected AspectData() {}

  @Id
  @ToString.Include
  @GeneratedValue(generator = "UUID")
  @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
  private String dbId;

  @ManyToOne(fetch = FetchType.LAZY, targetEntity = DomainBaseData.class, optional = false)
  @JoinColumn(name = "domain_id")
  private DomainBase domain;

  @ManyToOne(fetch = FetchType.LAZY, targetEntity = ElementData.class, optional = false)
  private Element owner;

  @Override
  public boolean equals(Object o) {
    if (o == null) return false;

    if (this == o) return true;

    if (!(o instanceof AspectData)) return false;

    AspectData other = (AspectData) o;
    // Transient (unmanaged) entities have an ID of 'null'. Only managed
    // (persisted and detached) entities have an identity. JPA requires that
    // an entity's identity remains the same over all state changes.
    // Therefore a transient entity must never equal another entity.
    String dbId = getDbId();
    return dbId != null && dbId.equals(other.getDbId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
