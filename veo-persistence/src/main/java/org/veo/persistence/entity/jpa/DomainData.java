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

import static java.time.Instant.now;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.validation.Valid;

import org.veo.core.entity.CatalogItem;
import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainTemplate;
import org.veo.core.entity.Profile;
import org.veo.core.entity.inspection.Inspection;
import org.veo.core.entity.risk.RiskDefinitionRef;
import org.veo.core.entity.riskdefinition.RiskDefinition;

import lombok.Data;
import lombok.ToString;

@Entity(name = "domain")
@Data
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@EntityListeners({VersionedEntityListener.class})
public class DomainData extends DomainBaseData implements Domain {
  @Column(name = "active")
  @ToString.Include
  private boolean active = true;

  @OneToMany(
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      targetEntity = CatalogItemData.class,
      mappedBy = "domain")
  @Valid
  private Set<CatalogItem> catalogItems = new HashSet<>();

  public void setProfiles(Set<Profile> profiles) {
    profiles.stream().map(ProfileData.class::cast).forEach(p -> p.setOwner(this));
    this.profiles = profiles;
  }

  @OneToMany(
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      targetEntity = ProfileData.class,
      mappedBy = "domain")
  @Valid
  private Set<Profile> profiles = new HashSet<>();

  @ManyToOne(targetEntity = DomainTemplateData.class, fetch = FetchType.LAZY)
  @Valid
  private DomainTemplate domainTemplate;

  // This enforces the composition association Client-Domain
  @ManyToOne(targetEntity = ClientData.class, optional = false, fetch = FetchType.LAZY)
  @Valid
  private Client owner;

  @Override
  public boolean isActive() {
    return active;
  }

  public void setCatalogItems(Set<CatalogItem> catalogItems) {
    catalogItems.forEach(ci -> ci.setDomainBase(this));
    this.catalogItems.clear();
    this.catalogItems.addAll(catalogItems);
  }

  @Override
  public boolean applyRiskDefinition(String riskDefinitionRef, RiskDefinition riskDefinition) {
    riskDefinition.validateRiskDefinition(this);
    var isNewRiskDef = riskDefinitionSet.apply(riskDefinitionRef, riskDefinition);
    setUpdatedAt(now());
    return isNewRiskDef;
  }

  @Override
  public void removeRiskDefinition(RiskDefinitionRef riskDefinition) {
    riskDefinitionSet.remove(riskDefinition.getIdRef());
    setUpdatedAt(now());
  }

  @Override
  public boolean applyInspection(String inspectionId, Inspection inspection) {
    return inspectionSet.apply(inspectionId, inspection);
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
