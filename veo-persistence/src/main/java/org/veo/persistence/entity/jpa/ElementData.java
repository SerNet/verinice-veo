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

import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedAttributeNode;
import javax.persistence.NamedEntityGraph;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Transient;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.hibernate.annotations.Formula;
import org.hibernate.annotations.GenericGenerator;

import org.veo.core.entity.CatalogItem;
import org.veo.core.entity.CustomAspect;
import org.veo.core.entity.CustomLink;
import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainTemplate;
import org.veo.core.entity.Element;
import org.veo.core.entity.InvalidSubTypeException;
import org.veo.core.entity.Nameable;
import org.veo.core.entity.Scope;
import org.veo.core.entity.Unit;
import org.veo.core.entity.aspects.Aspect;
import org.veo.core.entity.aspects.SubTypeAspect;
import org.veo.core.entity.decision.DecisionRef;
import org.veo.core.entity.decision.DecisionResult;
import org.veo.persistence.entity.jpa.validation.HasOwnerOrContainingCatalogItem;

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import lombok.ToString;

@Entity(name = "element")
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@Data
@NamedEntityGraph(
    name = ElementData.FULL_AGGREGATE_GRAPH,
    attributeNodes = {
      @NamedAttributeNode(value = "customAspects"),
      @NamedAttributeNode(value = "domains"),
      @NamedAttributeNode(value = "appliedCatalogItems"),
      @NamedAttributeNode(value = "links"),
      @NamedAttributeNode(value = "decisionResultsAspects"),
      @NamedAttributeNode(value = "subTypeAspects")
    })
@HasOwnerOrContainingCatalogItem
public abstract class ElementData extends IdentifiableVersionedData
    implements NameableData, Element {

  public static final String FULL_AGGREGATE_GRAPH = "fullAggregateGraph";

  @Id
  @ToString.Include
  @GeneratedValue(generator = "UUID")
  @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
  private String dbId;

  @NotNull
  @Column(name = "name")
  @ToString.Include
  private String name;

  @Column(name = "abbreviation")
  private String abbreviation;

  @Column(name = "description", length = Nameable.DESCRIPTION_MAX_LENGTH)
  private String description;

  @NotNull
  @Column(name = "designator")
  @ToString.Include
  @Pattern(regexp = "([A-Z]{3}-\\d+)|NO_DESIGNATOR")
  private String designator;

  @Column(name = "domains")
  @ManyToMany(targetEntity = DomainData.class, fetch = FetchType.LAZY)
  private final Set<Domain> domains = new HashSet<>();

  @Column(name = "links")
  @OneToMany(
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      targetEntity = CustomLinkData.class,
      mappedBy = "source",
      fetch = FetchType.LAZY)
  @Valid
  private final Set<CustomLink> links = new HashSet<>();

  @Column(name = "customaspects")
  @OneToMany(
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      targetEntity = CustomAspectData.class,
      mappedBy = "owner",
      fetch = FetchType.LAZY)
  @Valid
  private final Set<CustomAspect> customAspects = new HashSet<>();

  @ManyToMany(targetEntity = ScopeData.class, mappedBy = "members", fetch = FetchType.LAZY)
  private final Set<Scope> scopes = new HashSet<>();

  @Column(name = "sub_type_aspects")
  @OneToMany(
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      targetEntity = SubTypeAspectData.class,
      mappedBy = "owner",
      fetch = FetchType.LAZY)
  @Valid
  private Set<SubTypeAspect> subTypeAspects = new HashSet<>();

  @Column(name = "decision_results_aspect")
  @OneToMany(
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      targetEntity = DecisionResultsAspectData.class,
      mappedBy = "owner",
      fetch = FetchType.LAZY)
  @Valid
  private final Set<DecisionResultsAspectData> decisionResultsAspects = new HashSet<>();

  @ManyToMany(targetEntity = CatalogItemData.class, fetch = FetchType.LAZY)
  private Set<CatalogItem> appliedCatalogItems = new HashSet<>();

  @ManyToOne(fetch = FetchType.LAZY, targetEntity = UnitData.class)
  @JoinColumn(name = "owner_id")
  private Unit owner;

  @OneToOne(fetch = FetchType.LAZY, targetEntity = CatalogItemData.class)
  @JoinColumn(name = "containing_catalog_item_id")
  private CatalogItem containingCatalogItem;

  @Formula(
      "case when abbreviation is null then concat(designator,' ',name) else concat(designator,' ',abbreviation,' ',name) end")
  @Setter(AccessLevel.NONE)
  private String displayName;

  @Formula("length(designator)")
  @Setter(AccessLevel.NONE)
  private String designatorLength;

  protected <T extends Aspect> Optional<T> findAspectByDomain(
      Set<T> source, DomainTemplate domain) {
    return source.stream().filter(aspect -> aspect.getDomain() == domain).findFirst();
  }

  @Override
  public Optional<String> getSubType(DomainTemplate domain) {
    return findAspectByDomain(subTypeAspects, domain).map(SubTypeAspect::getSubType);
  }

  @Override
  public Optional<String> getStatus(DomainTemplate domain) {
    return findAspectByDomain(subTypeAspects, domain).map(SubTypeAspect::getStatus);
  }

  @Override
  public void setSubType(DomainTemplate domain, String subType, String status) {
    removeAspect(subTypeAspects, domain);
    if (subType != null) {
      subTypeAspects.add(new SubTypeAspectData(domain, this, subType, status));
    } else if (status != null) {
      throw new InvalidSubTypeException(
          String.format(
              "Cannot assign status %s for domain %s without a sub type", status, domain.getId()));
    }
  }

  public void setLinks(Set<CustomLink> newLinks) {
    links.clear();
    newLinks.forEach(l -> l.setSource(this));
    links.addAll(newLinks);
  }

  public void setCustomAspects(Set<CustomAspect> aCustomAspects) {
    this.customAspects.clear();
    aCustomAspects.forEach(
        aspect -> {
          if (aspect instanceof CustomAspectData) {
            ((CustomAspectData) aspect).setOwner(this);
          }
        });
    this.customAspects.addAll(aCustomAspects);
  }

  @Override
  public Map<DecisionRef, DecisionResult> getDecisionResults(DomainTemplate domain) {
    return findAspectByDomain(decisionResultsAspects, domain)
        .map(DecisionResultsAspectData::getResults)
        .orElse(Map.of());
  }

  @Override
  public void setDecisionResults(Map<DecisionRef, DecisionResult> results, Domain domain) {
    removeAspect(decisionResultsAspects, domain);
    decisionResultsAspects.add(new DecisionResultsAspectData(domain, this, results));
  }

  /**
   * Add the given CustomLink to the collection of links. Manages the association between Element
   * and CustomLink.
   *
   * @return true if the link was successfully added
   */
  public boolean addToLinks(CustomLink aCustomLink) {
    aCustomLink.setSource(this);
    return this.links.add(aCustomLink);
  }

  /**
   * Remove the given CustomLink from the collection links.
   *
   * @return true if removed
   */
  @Override
  public boolean removeFromLinks(CustomLink aCustomLink) {
    forceUpdate();
    aCustomLink.setSource(null);
    return this.links.remove(aCustomLink);
  }

  /**
   * Add the given {@link CustomAspect} to the collection customAspects.
   *
   * @return true if added
   */
  public boolean addToCustomAspects(CustomAspect aCustomAspect) {
    if (aCustomAspect instanceof CustomAspectData) {
      ((CustomAspectData) aCustomAspect).setOwner(this);
    }
    return this.customAspects.add(aCustomAspect);
  }

  /**
   * Remove the given {@link CustomAspect} from the collection customAspects.
   *
   * @return true if removed
   */
  public boolean removeFromCustomAspects(CustomAspect aCustomAspect) {
    if (aCustomAspect instanceof CustomAspectData) {
      CustomAspectData propertiesData = (CustomAspectData) aCustomAspect;
      propertiesData.setOwner(null);
    }
    return this.customAspects.remove(aCustomAspect);
  }

  @Transient
  @Override
  public String getDisplayName() {
    return displayName;
  }

  private void removeAspect(Set<? extends Aspect> aspects, DomainTemplate domain) {
    aspects.removeIf(a -> a.getDomain().equals(domain));
  }

  /**
   * Convince JPA that this entity has been changed. This may be necessary when making changes to a
   * collection.
   */
  private void forceUpdate() {
    this.setUpdatedAt(Instant.now());
  }
}
