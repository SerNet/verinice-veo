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

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.validation.Valid;

import org.veo.core.entity.CatalogItem;
import org.veo.core.entity.DomainTemplate;
import org.veo.core.entity.Nameable;
import org.veo.core.entity.Profile;

import lombok.Data;
import lombok.ToString;

@Entity(name = "domaintemplate")
@ToString(onlyExplicitlyIncluded = true)
@Data
public class DomainTemplateData extends DomainBaseData implements DomainTemplate, Nameable {
  @OneToMany(
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      targetEntity = CatalogItemData.class,
      mappedBy = "domainTemplate")
  @Valid
  private Set<CatalogItem> catalogItems = new HashSet<>();

  @Override
  public void setProfiles(Set<Profile> profiles) {
    profiles.stream().map(ProfileData.class::cast).forEach(p -> p.setOwner(this));
    this.profiles = profiles;
  }

  @OneToMany(
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      targetEntity = ProfileData.class,
      mappedBy = "domainTemplate")
  @Valid
  private Set<Profile> profiles = new HashSet<>();

  @Override
  public void setCatalogItems(Set<CatalogItem> catalogItems) {
    catalogItems.forEach(ci -> ci.setDomainBase(this));
    this.catalogItems.clear();
    this.catalogItems.addAll(catalogItems);
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
