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
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.validation.Valid;

import org.hibernate.annotations.GenericGenerator;

import org.veo.core.entity.CatalogItem;
import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainBase;
import org.veo.core.entity.DomainTemplate;
import org.veo.core.entity.Element;
import org.veo.core.entity.TailoringReference;
import org.veo.core.entity.TailoringReferenceType;
import org.veo.core.entity.Unit;
import org.veo.core.entity.UpdateReference;

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity(name = "catalogitem")
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@Data
public class CatalogItemData extends TemplateItemData<CatalogItem> implements CatalogItem {

  @Id
  @ToString.Include
  @GeneratedValue(generator = "UUID")
  @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
  private String dbId;

  @ManyToOne(targetEntity = DomainData.class)
  @Getter(value = AccessLevel.NONE)
  @Setter(value = AccessLevel.NONE)
  private Domain domain;

  @ManyToOne(targetEntity = DomainTemplateData.class)
  @Getter(value = AccessLevel.NONE)
  @Setter(value = AccessLevel.NONE)
  private DomainTemplate domainTemplate;

  @Column(name = "tailoringreferences")
  @OneToMany(
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      targetEntity = CatalogTailoringReferenceData.class,
      mappedBy = "owner",
      fetch = FetchType.LAZY)
  @Valid
  private Set<TailoringReference<CatalogItem>> tailoringReferences = new HashSet<>();

  @Column(name = "updatereferences")
  @OneToMany(
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      targetEntity = UpdateReferenceData.class,
      mappedBy = "owner",
      fetch = FetchType.LAZY)
  @Valid
  private Set<UpdateReference> updateReferences = new HashSet<>();

  public DomainBase getDomainBase() {
    return domain != null ? domain : domainTemplate;
  }

  public void setDomainBase(DomainBase owner) {
    if (getDomainBase() != null && !getDomainBase().equals(owner)) {
      throw new IllegalArgumentException("Cannot move catalog item between domains");
    }

    if (owner instanceof Domain d) {
      this.domain = d;
    } else if (owner instanceof DomainTemplate dt) {
      this.domainTemplate = dt;
    } else {
      throw new IllegalArgumentException("Unexpected domain type");
    }
  }

  /** create an instance of the described element* */
  @Override
  public Element incarnate(Unit owner) {
    requireDomainMembership();

    Element element = createElement(owner);
    element.setName(name);
    element.setDescription(description);
    element.setAbbreviation(abbreviation);
    element.apply(this);
    element.getAppliedCatalogItems().add(this);
    return element;
  }

  @Override
  public TailoringReference<CatalogItem> addTailoringReference(
      TailoringReferenceType referenceType, CatalogItem referenceTarget) {
    var reference =
        switch (referenceType) {
          case LINK, LINK_EXTERNAL -> new LinkTailoringReferenceData();
          case RISK -> throw new UnsupportedOperationException(
              "Risks currently not supported for catalog items");
          default -> new CatalogTailoringReferenceData();
        };
    reference.setReferenceType(referenceType);
    reference.setOwner(this);
    reference.setTarget(referenceTarget);
    this.tailoringReferences.add(reference);
    return reference;
  }
}
