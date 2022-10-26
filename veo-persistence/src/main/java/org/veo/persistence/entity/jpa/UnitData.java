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

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Nameable;
import org.veo.core.entity.Unit;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;

@Entity(name = "unit")
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@Data
public class UnitData extends ElementOwnerData implements Unit {

  @Column(name = "name")
  @ToString.Include
  private String name;

  @Column(name = "abbreviation")
  private String abbreviation;

  @Column(name = "description", length = Nameable.DESCRIPTION_MAX_LENGTH)
  private String description;

  @ManyToOne(fetch = FetchType.LAZY, targetEntity = ClientData.class)
  @JoinColumn(name = "client_id")
  private Client client;

  @OneToMany(
      mappedBy = "parent",
      fetch = FetchType.LAZY,
      targetEntity = UnitData.class,
      cascade = CascadeType.ALL)
  @Valid
  private final Set<Unit> units = new HashSet<>();

  @ManyToOne(targetEntity = UnitData.class)
  private Unit parent;

  @Column(name = "domains")
  @ManyToMany(targetEntity = DomainData.class)
  private Set<Domain> domains = new HashSet<>();

  @Override
  public void setUnits(Set<Unit> units) {
    units.forEach(u -> u.setParent(this));
    this.units.clear();
    this.units.addAll(units);
  }

  public void setDomains(Set<Domain> newDomains) {
    domains.clear();
    domains.addAll(newDomains);
  }

  public boolean addToDomains(Domain aDomain) {
    return this.domains.add(aDomain);
  }

  public boolean addToDomains(@NotNull @NonNull Set<Domain> domains) {
    return this.domains.addAll(domains);
  }

  /**
   * Remove the given Domain from the collection domains.
   *
   * @return true if removed
   */
  public boolean removeFromDomains(Domain aDomain) {
    return this.domains.remove(aDomain);
  }
}
