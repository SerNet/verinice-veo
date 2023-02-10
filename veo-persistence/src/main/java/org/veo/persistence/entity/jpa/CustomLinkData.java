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

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.veo.core.entity.CustomLink;
import org.veo.core.entity.Element;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity(name = "customlink")
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@Data
public class CustomLinkData extends CustomAspectData implements CustomLink {

  @ManyToOne(fetch = FetchType.LAZY, targetEntity = ElementData.class, optional = true) // due to
  // the
  // single-table
  // inheritance
  // mapping,
  // this
  // must be
  // nullable
  @JoinColumn(name = "target_id")
  private Element target;

  @ManyToOne(fetch = FetchType.LAZY, targetEntity = ElementData.class, optional = true) // due to
  // the
  // single-table
  // inheritance
  // mapping,
  // this
  // must be
  // nullable
  @JoinColumn(name = "source_id")
  private Element source;

  public void setSource(Element aSource) {
    this.source = aSource;
  }

  @Override
  public void remove() {
    this.getSource().removeFromLinks(this);
  }
}
