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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import com.vladmihalcea.hibernate.type.json.JsonType;

import org.veo.core.entity.CustomAspect;
import org.veo.core.entity.DomainBase;
import org.veo.core.entity.Element;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity(name = "custom_aspect")
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@Data
@TypeDef(name = "json", typeClass = JsonType.class)
public class CustomAspectData implements CustomAspect {

  public CustomAspectData(String type, Map<String, Object> attributes, DomainBase domain) {
    this.type = type;
    this.attributes = attributes;
    this.domain = domain;
  }

  @Id @ToString.Include private String dbId = UUID.randomUUID().toString();

  @ToString.Include private String type;

  @ManyToOne(
      fetch = FetchType.LAZY,
      targetEntity = ElementData.class,
      // 'links' are also custom aspects, saved in the same table but mapped by
      // 'source'
      // column, due to the single-table inheritance mapping used here.
      // 'owner' must therefore be nullable for these entities:
      optional = true)
  private Element owner;

  @ManyToOne(fetch = FetchType.LAZY, targetEntity = DomainBaseData.class, optional = false)
  @JoinColumn(name = "domain_id")
  private DomainBase domain;

  @Type(type = "json")
  @Column(columnDefinition = "jsonb")
  private Map<String, Object> attributes = new HashMap<>();

  @Override
  public boolean setAttributes(@NonNull Map<String, Object> newAttributes) {
    if (!attributes.equals(newAttributes)) {
      attributes = newAttributes;
      return true;
    }
    return false;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null) return false;

    if (this == o) return true;

    if (!(o instanceof CustomAspectData)) return false;

    CustomAspectData other = (CustomAspectData) o;
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
