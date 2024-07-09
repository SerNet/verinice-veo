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
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.validation.Valid;

import org.hibernate.annotations.Where;

import org.veo.core.entity.Client;
import org.veo.core.entity.ClientState;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Nameable;
import org.veo.core.entity.event.ClientEvent.ClientChangeType;
import org.veo.core.entity.specification.ClientBoundaryViolationException;

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import lombok.ToString;

@Entity(name = "client")
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@Data
@EntityListeners({VersionedEntityListener.class})
public class ClientData extends IdentifiableVersionedData implements Client, Nameable {

  @Id @ToString.Include private UUID dbId;

  @Column(name = "name")
  @ToString.Include
  private String name;

  @Column(name = "abbreviation")
  private String abbreviation;

  @Column(name = "description", length = Nameable.DESCRIPTION_MAX_LENGTH)
  private String description;

  @Column(name = "total_units")
  @Setter(value = AccessLevel.NONE)
  private int totalUnits;

  @Column(name = "state")
  @Setter(value = AccessLevel.NONE)
  @Enumerated(EnumType.STRING)
  @ToString.Include
  private ClientState state = ClientState.CREATED;

  @Column(name = "max_units")
  private int maxUnits;

  @Column(name = "domains")
  @OneToMany(
      mappedBy = "owner",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      targetEntity = DomainData.class)
  @Where(clause = "active = true")
  @Valid
  private final Set<Domain> domains = new HashSet<>();

  public boolean updateState(ClientChangeType changeType) {
    if (state.isValidChange(changeType)) {
      ClientState nextState = state.nextState(changeType);
      ClientState oldState = state;
      state = nextState;
      return state.equals(oldState);
    }
    throw new IllegalStateException(
        "The client state: "
            + state.name()
            + " does not support the change type: "
            + changeType.name());
  }

  public void setDomains(Set<Domain> newDomains) {
    domains.clear();
    newDomains.forEach(domain -> domain.setOwner(this));
    domains.addAll(newDomains);
  }

  // Only returns active domains
  public Set<Domain> getDomains() {
    return domains;
  }

  /**
   * Add the given Domain to the collection domains.
   *
   * @return true if added
   */
  public boolean addToDomains(Domain aDomain) {
    aDomain.setOwner(this);
    return this.domains.add(aDomain);
  }

  /**
   * Remove the given Domain from the collection domains.
   *
   * @return true if removed
   */
  public boolean removeFromDomains(Domain aDomain) {
    if (!aDomain.getOwner().equals(this)) throw new ClientBoundaryViolationException(aDomain, this);
    aDomain.setOwner(null);
    return this.domains.remove(aDomain);
  }

  @Override
  public void incrementTotalUnits() {
    totalUnits++;
  }

  @Override
  public void decrementTotalUnits() {
    totalUnits--;
  }
}
