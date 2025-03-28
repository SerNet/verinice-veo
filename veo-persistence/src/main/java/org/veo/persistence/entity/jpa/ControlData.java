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

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.validation.Valid;

import org.veo.core.entity.Control;
import org.veo.core.entity.Domain;
import org.veo.core.entity.TemplateItemAspects;

import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Entity(name = "control")
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
public class ControlData extends ElementData implements Control {

  @ManyToMany(
      targetEntity = ControlData.class,
      cascade = {CascadeType.PERSIST, CascadeType.MERGE})
  @JoinTable(
      name = "control_parts",
      joinColumns = @JoinColumn(name = "composite_id"),
      inverseJoinColumns = @JoinColumn(name = "part_id"))
  @Valid
  @Getter
  private final Set<Control> parts = new HashSet<>();

  @ManyToMany(targetEntity = ControlData.class, mappedBy = "parts", fetch = FetchType.LAZY)
  @Getter
  private final Set<Control> composites = new HashSet<>();

  @Override
  protected void applyItemAspects(TemplateItemAspects itemAspects, Domain domain) {}

  @Override
  public boolean equals(Object obj) {
    return super.equals(obj);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }
}
