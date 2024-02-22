/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Urs Zeidler
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
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.validation.Valid;

import org.hibernate.annotations.GenericGenerator;

import org.veo.core.entity.CatalogItem;
import org.veo.core.entity.ControlImplementationTailoringReference;
import org.veo.core.entity.Element;
import org.veo.core.entity.LinkTailoringReference;
import org.veo.core.entity.Profile;
import org.veo.core.entity.ProfileItem;
import org.veo.core.entity.RiskTailoringReference;
import org.veo.core.entity.TailoringReference;
import org.veo.core.entity.TailoringReferenceType;
import org.veo.core.entity.Unit;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@Entity(name = "profile_item")
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
public class ProfileItemData extends TemplateItemData<ProfileItem> implements ProfileItem {
  @Id
  @ToString.Include
  @GeneratedValue(generator = "UUID")
  @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
  private String dbId;

  @ManyToOne(targetEntity = CatalogItemData.class, fetch = FetchType.LAZY)
  private CatalogItem appliedCatalogItem;

  @ManyToOne(targetEntity = ProfileData.class, optional = false)
  private Profile owner;

  @OneToMany(
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      targetEntity = ProfileTailoringReferenceData.class,
      mappedBy = "owner",
      fetch = FetchType.LAZY)
  @Valid
  private Set<TailoringReference<ProfileItem>> tailoringReferences = new HashSet<>();

  /** create an instance of the described element* */
  @Override
  public Element incarnate(Unit owner) {
    requireDomainMembership();

    Element element = createElement(owner);
    element.setName(name);
    element.setDescription(description);
    element.setAbbreviation(abbreviation);
    element.apply(this);
    if (getAppliedCatalogItem() != null) {
      element.getAppliedCatalogItems().add(getAppliedCatalogItem());
    }
    return element;
  }

  @Override
  protected void add(
      TailoringReference<ProfileItem> reference, TailoringReferenceType type, ProfileItem target) {
    super.add(reference, type, target);
    this.tailoringReferences.add(reference);
  }

  @Override
  protected TailoringReference<ProfileItem> createTailoringReference() {
    return new ProfileTailoringReferenceData();
  }

  @Override
  protected LinkTailoringReference<ProfileItem> createLinkTailoringReference() {
    return new ProfileLinkTailoringReferenceData();
  }

  @Override
  protected RiskTailoringReference<ProfileItem> createRiskTailoringReference() {
    return new ProfileRiskTailoringReferenceData();
  }

  @Override
  protected ControlImplementationTailoringReference<ProfileItem>
      createControlImplementationTailoringReference() {
    return new ProfileControlImplementationTailoringReferenceData();
  }
}
