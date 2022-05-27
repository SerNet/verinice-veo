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

import java.util.Optional;
import java.util.UUID;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;

import org.hibernate.annotations.GenericGenerator;

import org.veo.core.entity.CatalogItem;
import org.veo.core.entity.CatalogReference;
import org.veo.core.entity.Identifiable;
import org.veo.core.entity.Key;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@MappedSuperclass
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Data
@SuppressWarnings("PMD.AbstractClassWithoutAnyMethod")
public abstract class CatalogReferenceData implements Identifiable, CatalogReference {
  @Id
  @ToString.Include
  @GeneratedValue(generator = "UUID")
  @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
  private String dbId;

  @EqualsAndHashCode.Include
  @ManyToOne(targetEntity = CatalogItemData.class)
  private CatalogItem catalogItem;

  @ManyToOne(targetEntity = CatalogItemData.class, optional = false)
  private CatalogItem owner;

  @Override
  public Key<UUID> getId() {
    return Key.uuidFrom(getDbId());
  }

  @Override
  public void setId(Key<UUID> id) {
    setDbId(Optional.ofNullable(id).map(Key::uuidValue).orElse(null));
  }
}
