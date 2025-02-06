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

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;

import org.veo.core.entity.Domain;
import org.veo.core.entity.Incident;
import org.veo.core.entity.TemplateItemAspects;

import lombok.Getter;
import lombok.ToString;

@Entity(name = "INCIDENT")
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
public class IncidentData extends ElementData implements Incident {

  @ManyToMany(
      targetEntity = IncidentData.class,
      cascade = {CascadeType.PERSIST, CascadeType.MERGE})
  @JoinTable(
      name = "incident_parts",
      joinColumns = @JoinColumn(name = "composite_id"),
      inverseJoinColumns = @JoinColumn(name = "part_id"))
  @Getter
  private final Set<Incident> parts = new HashSet<>();

  @ManyToMany(targetEntity = IncidentData.class, mappedBy = "parts", fetch = FetchType.LAZY)
  @Getter
  private final Set<Incident> composites = new HashSet<>();

  @Override
  protected void applyItemAspects(TemplateItemAspects itemAspects, Domain domain) {
    // GNDN
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
