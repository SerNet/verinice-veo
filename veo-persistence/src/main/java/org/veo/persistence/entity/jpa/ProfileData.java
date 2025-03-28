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
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.UuidGenerator;

import org.veo.core.entity.Client;
import org.veo.core.entity.Displayable;
import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainBase;
import org.veo.core.entity.DomainTemplate;
import org.veo.core.entity.Nameable;
import org.veo.core.entity.Profile;
import org.veo.core.entity.ProfileItem;
import org.veo.core.entity.ProfileState;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity(name = "profile")
@Table(
    uniqueConstraints = {
      @UniqueConstraint(
          name = "UK_domain_product_id_language",
          columnNames = {"product_id", "language", "domain_db_id"}),
      @UniqueConstraint(
          name = "UK_domain_template_product_id_language",
          columnNames = {"product_id", "language", "domain_template_db_id"}),
    })
@Data
@ToString(onlyExplicitlyIncluded = true)
@SuppressFBWarnings("PI_DO_NOT_REUSE_PUBLIC_IDENTIFIERS_CLASS_NAMES")
public class ProfileData extends IdentifiableVersionedData implements Profile, Displayable {
  @Id
  @ToString.Include
  @GeneratedValue(generator = "UUID")
  @UuidGenerator
  @EqualsAndHashCode.Include
  @Column(name = "db_id")
  private UUID id;

  @ToString.Include
  @Column(name = "name")
  private String name;

  @Column(name = "description", length = Nameable.DESCRIPTION_MAX_LENGTH)
  private String description;

  @Column(name = "language", length = 20)
  private String language;

  @Column(length = ProfileState.PRODUCT_ID_MAX_LENGTH)
  private String productId;

  @Override
  public void setItems(Set<ProfileItem> items) {
    items.stream().map(i -> (ProfileItemData) i).forEach(i -> i.setOwner(this));
    this.items = items;
  }

  @OneToMany(
      cascade = CascadeType.ALL,
      fetch = FetchType.LAZY,
      mappedBy = "owner",
      targetEntity = ProfileItemData.class,
      orphanRemoval = true)
  private Set<ProfileItem> items = new HashSet<>();

  @ManyToOne(targetEntity = DomainData.class)
  @Getter(value = AccessLevel.NONE)
  @Setter(value = AccessLevel.NONE)
  private Domain domain;

  @ManyToOne(targetEntity = DomainTemplateData.class)
  @Getter(value = AccessLevel.NONE)
  @Setter(value = AccessLevel.NONE)
  private DomainTemplate domainTemplate;

  @Override
  public DomainBase getOwner() {
    return domain != null ? domain : domainTemplate;
  }

  @Override
  public void setOwner(DomainBase owner) {
    if (getOwner() != null && !getOwner().equals(owner)) {
      throw new IllegalArgumentException("Cannot move profiles between domains");
    }

    if (owner instanceof Domain d) {
      this.domain = d;
    } else if (owner instanceof DomainTemplate dt) {
      this.domainTemplate = dt;
    } else {
      throw new IllegalArgumentException("Unexpected domain type");
    }
  }

  @Override
  public Optional<Client> getOwningClient() {
    if (domain == null) return Optional.empty();
    else return domain.getOwningClient();
  }

  @Override
  public String getDisplayName() {
    return name;
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
