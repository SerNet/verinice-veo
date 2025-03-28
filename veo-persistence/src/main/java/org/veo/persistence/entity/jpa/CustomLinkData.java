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

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import org.hibernate.Hibernate;

import org.veo.core.entity.CustomLink;
import org.veo.core.entity.Element;

import lombok.Data;
import lombok.ToString;

@Entity(name = "customlink")
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@Data
public class CustomLinkData extends CustomAttributeContainerData implements CustomLink {
  public CustomLinkData() {
    super();
  }

  @Override
  public Element loadTarget() {
    return (Element) Hibernate.unproxy(target);
  }

  @ManyToOne(fetch = FetchType.LAZY, targetEntity = ElementData.class, optional = false)
  @JoinColumn(name = "target_id")
  private Element target;

  @ManyToOne(fetch = FetchType.LAZY, targetEntity = ElementData.class, optional = false)
  @JoinColumn(name = "source_id")
  private Element source;

  @Override
  public void setSource(Element aSource) {
    this.source = aSource;
  }

  @Override
  public void remove() {
    this.getSource().removeLink(this);
  }

  @Override
  public boolean equals(Object obj) {
    return super.equals(obj);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }
}
