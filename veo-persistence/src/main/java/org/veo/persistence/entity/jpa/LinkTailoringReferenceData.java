/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Urs Zeidler
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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;

import org.hibernate.annotations.Type;

import org.veo.core.entity.CatalogItem;
import org.veo.core.entity.DomainBase;
import org.veo.core.entity.LinkTailoringReference;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import lombok.Data;
import lombok.ToString;

@Entity(name = "linktailoringreference")
@Data
public class LinkTailoringReferenceData extends CatalogTailoringReferenceData
    implements LinkTailoringReference<CatalogItem, DomainBase> {

  @ToString.Include private String linkType;

  @Type(JsonType.class)
  @Column(columnDefinition = "jsonb")
  private Map<String, Object> attributes = new HashMap<>();

  @Override
  public boolean equals(Object obj) {
    return super.equals(obj);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }
}
