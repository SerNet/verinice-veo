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

import org.veo.core.entity.Catalog;
import org.veo.core.entity.CatalogItem;
import org.veo.core.entity.DomainTemplate;
import org.veo.core.entity.Element;
import org.veo.core.entity.TailoringReference;
import org.veo.core.entity.UpdateReference;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity(name = "catalogitem")
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@Data
public class CatalogItemData extends TemplateItemData implements CatalogItem {

  @Id
  @ToString.Include
  @GeneratedValue(generator = "UUID")
  @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
  private String dbId;

  @ManyToOne(targetEntity = CatalogData.class, optional = false)
  private Catalog catalog;

  @Override
  public void setCatalog(Catalog catalog) {
    this.catalog = catalog;
    catalog.getCatalogItems().add(this);
  }

  @Column(name = "tailoringreferences")
  @OneToMany(
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      targetEntity = TailoringReferenceData.class,
      mappedBy = "owner",
      fetch = FetchType.LAZY)
  @Valid
  private Set<TailoringReference> tailoringReferences = new HashSet<>();

  @Column(name = "updatereferences")
  @OneToMany(
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      targetEntity = UpdateReferenceData.class,
      mappedBy = "owner",
      fetch = FetchType.LAZY)
  @Valid
  private Set<UpdateReference> updateReferences = new HashSet<>();

  /** create an instance of the described element* */
  @Override
  public Element incarnate() {
    if (getDomain().getModelInterface().equals(DomainTemplate.class)) {
      throw new IllegalArgumentException(
          "Can not incarnate a catalog item from a domain template.");
    }

    Element element = createElement();
    element.setName(name);
    element.setDescription(description);
    element.setAbbreviation(abbreviation);
    element.apply(this);
    return element;
  }
}
