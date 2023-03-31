/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Alexander Koderman.
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

import javax.annotation.Nullable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.GenericGenerator;

import org.veo.core.entity.AbstractRisk;
import org.veo.core.entity.Control;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Person;
import org.veo.core.entity.RiskAffected;
import org.veo.core.entity.Scenario;
import org.veo.core.entity.exception.ModelConsistencyException;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity(name = "abstractriskdata")
@ToString(onlyExplicitlyIncluded = true)
@Getter
@Setter
@RequiredArgsConstructor
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public abstract class AbstractRiskData<T extends RiskAffected<T, R>, R extends AbstractRisk<T, R>>
    extends VersionedData implements AbstractRisk<T, R> {

  @Id
  @ToString.Include
  @GeneratedValue(generator = "UUID")
  @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
  private String dbId;

  @Column(name = "designator")
  @ToString.Include
  private String designator;

  @Column(name = "domains")
  @ManyToMany(targetEntity = DomainData.class, fetch = FetchType.LAZY)
  @ToString.Exclude
  private final Set<Domain> domains = new HashSet<>();

  @ManyToOne(fetch = FetchType.LAZY, targetEntity = ControlData.class)
  @JoinColumn(name = "control_id")
  @Setter(AccessLevel.PRIVATE)
  @ToString.Exclude
  private Control mitigation;

  @NotNull
  @NonNull
  @ManyToOne(fetch = FetchType.LAZY, targetEntity = ScenarioData.class, optional = false)
  @Setter(AccessLevel.PROTECTED)
  @ToString.Exclude
  private Scenario scenario;

  @ManyToOne(fetch = FetchType.LAZY, targetEntity = PersonData.class)
  @JoinColumn(name = "person_id")
  @Setter(AccessLevel.PRIVATE)
  @ToString.Exclude
  private Person riskOwner;

  @NotNull
  @NonNull
  @ManyToOne(fetch = FetchType.LAZY, targetEntity = RiskAffectedData.class, optional = false)
  @Setter(AccessLevel.PRIVATE)
  @ToString.Exclude
  private T entity;

  @Override
  public boolean addToDomains(Domain aDomain) {
    checkDomain(aDomain);
    return domains.add(aDomain);
  }

  private void checkDomain(Domain aDomain) {
    if (!getEntity().getDomains().contains(aDomain)) {
      throw new ModelConsistencyException(
          "The provided domain '%s' is not yet known to the entity", aDomain.getDisplayName());
    }
  }

  @Override
  public boolean removeFromDomains(Domain aDomain) {
    if (domains.size() < 2) {
      throw new ModelConsistencyException(
          "Could not remove domain '%s': cannot remove last domain from risk.", aDomain);
    }
    return domains.remove(aDomain);
  }

  public void setDomains(@NonNull @NotEmpty Set<Domain> newDomains) {
    if (newDomains.isEmpty())
      throw new IllegalArgumentException("There must be at least one domain for the risk.");
    this.domains.clear();
    this.domains.addAll(newDomains);
  }

  @Override
  public R mitigate(@Nullable Control control) {
    setMitigation(control);
    return (R) this;
  }

  @Override
  public R appoint(@Nullable Person riskOwner) {
    setRiskOwner(riskOwner);
    return (R) this;
  }

  @Override
  public void transferToDomain(Domain oldDomain, Domain newDomain) {
    if (domains.remove(oldDomain)) {
      domains.add(newDomain);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (o == null) return false;

    if (this == o) return true;

    if (!(o instanceof AbstractRiskData)) return false;

    AbstractRiskData other = (AbstractRiskData) o;
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
