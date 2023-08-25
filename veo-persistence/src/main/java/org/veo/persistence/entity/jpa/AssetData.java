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

import org.veo.core.entity.Asset;
import org.veo.core.entity.AssetRisk;
import org.veo.core.entity.CatalogItem;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Profile;
import org.veo.core.entity.ProfileItem;
import org.veo.core.entity.Scenario;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Entity(name = "asset")
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
public class AssetData extends RiskAffectedData<Asset, AssetRisk> implements Asset {

  @ManyToMany(
      targetEntity = AssetData.class,
      cascade = {CascadeType.PERSIST, CascadeType.MERGE})
  @JoinTable(
      name = "asset_parts",
      joinColumns = @JoinColumn(name = "composite_id"),
      inverseJoinColumns = @JoinColumn(name = "part_id"))
  @Valid
  @Getter
  private final Set<Asset> parts = new HashSet<>();

  @Override
  AssetRisk createRisk(Scenario scenario) {
    return new AssetRiskData(this, scenario);
  }

  @ManyToMany(targetEntity = AssetData.class, mappedBy = "parts", fetch = FetchType.LAZY)
  @Getter
  private final Set<Asset> composites = new HashSet<>();

  @Override
  public CatalogItem toCatalogItem(Domain domain) {
    CatalogItem item = super.toCatalogItem(domain);
    getImpactValues(domain)
        .ifPresent(
            m -> { // TODO: verinice-veo#2285 set the impact values to the catalog item
              log.info("Ignoring Impactvalues for asset: {}", getIdAsString());
            });
    return item;
  }

  @Override
  public ProfileItem toProfileItem(Profile profile) {
    ProfileItem item = super.toProfileItem(profile);
    getImpactValues((Domain) profile.getOwner())
        .ifPresent(
            m -> { // TODO: verinice-veo#2285 set the impact values to the catalog item
              log.info(
                  "Ignoring Impactvalues for asset: {} in profile {}",
                  getIdAsString(),
                  profile.getName());
            });

    return item;
  }
}
