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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.Valid;

import org.veo.core.entity.CatalogItem;
import org.veo.core.entity.ControlImplementationTailoringReference;
import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainBase;
import org.veo.core.entity.DomainTemplate;
import org.veo.core.entity.Element;
import org.veo.core.entity.LinkTailoringReference;
import org.veo.core.entity.RequirementImplementationTailoringReference;
import org.veo.core.entity.RiskTailoringReference;
import org.veo.core.entity.TailoringReference;
import org.veo.core.entity.TailoringReferenceType;
import org.veo.core.entity.Unit;
import org.veo.core.entity.UpdateReference;
import org.veo.core.entity.exception.UnprocessableDataException;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity(name = "catalogitem")
@Table(
    uniqueConstraints = {
      @UniqueConstraint(
          name = "UK_symbolic_id_domain",
          columnNames = {"symbolic_db_id", "domain_db_id", "domain_template_db_id"}),
    })
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@Data
public class CatalogItemData extends TemplateItemData<CatalogItem, DomainBase>
    implements CatalogItem {
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
  private Set<TailoringReference<CatalogItem, DomainBase>> tailoringReferences = new HashSet<>();

  @Column(name = "updatereferences")
  @OneToMany(
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      targetEntity = UpdateReferenceData.class,
      mappedBy = "owner",
      fetch = FetchType.LAZY)
  @Valid
  private Set<UpdateReference> updateReferences = new HashSet<>();

  @Override
  public DomainBase getDomainBase() {
    return domain != null ? domain : domainTemplate;
  }

  @Override
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
    element.setAppliedCatalogItem(domain, this);
    return element;
  }

  @Override
  protected void add(
      TailoringReference<CatalogItem, DomainBase> reference,
      TailoringReferenceType type,
      CatalogItem target) {
    super.add(reference, type, target);
    tailoringReferences.add(reference);
  }

  @Override
  protected TailoringReference<CatalogItem, DomainBase> createTailoringReference() {
    return new CatalogTailoringReferenceData();
  }

  @Override
  protected LinkTailoringReference<CatalogItem, DomainBase> createLinkTailoringReference() {
    return new LinkTailoringReferenceData();
  }

  @Override
  protected RiskTailoringReference<CatalogItem, DomainBase> createRiskTailoringReference() {
    throw new UnprocessableDataException("Risks currently not supported for catalog items");
  }

  @Override
  protected ControlImplementationTailoringReference<CatalogItem, DomainBase>
      createControlImplementationTailoringReference() {
    throw new UnprocessableDataException(
        "Control implementations currently not supported for catalog items");
  }

  @Override
  protected RequirementImplementationTailoringReference<CatalogItem, DomainBase>
      createRequirementImplementationTailoringReference() {
    throw new UnprocessableDataException(
        "Requirement implementations currently not supported for catalog items");
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
