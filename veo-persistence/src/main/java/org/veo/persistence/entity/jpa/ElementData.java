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

import static java.util.List.copyOf;

import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Transient;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.hibernate.annotations.Formula;
import org.hibernate.annotations.GenericGenerator;

import org.veo.core.entity.CatalogItem;
import org.veo.core.entity.CustomAspect;
import org.veo.core.entity.CustomLink;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;
import org.veo.core.entity.Nameable;
import org.veo.core.entity.Scope;
import org.veo.core.entity.Unit;
import org.veo.core.entity.aspects.Aspect;
import org.veo.core.entity.aspects.SubTypeAspect;
import org.veo.core.entity.decision.DecisionRef;
import org.veo.core.entity.decision.DecisionResult;
import org.veo.core.entity.exception.EntityAlreadyExistsException;
import org.veo.core.entity.exception.UnprocessableDataException;

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

@Entity(name = "element")
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@Data
public abstract class ElementData extends IdentifiableVersionedData implements Element {

  @Id
  @ToString.Include
  @GeneratedValue(generator = "UUID")
  @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
  private String dbId;

  @Column(name = "name")
  @ToString.Include
  private String name;

  @Column(name = "abbreviation")
  private String abbreviation;

  @Column(name = "description", length = Nameable.DESCRIPTION_MAX_LENGTH)
  private String description;

  @Column(name = "designator")
  @ToString.Include
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

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, targetEntity = UnitData.class)
  @JoinColumn(name = "owner_id")
  private Unit owner;

  @Formula(
      "case when abbreviation is null then concat(designator,' ',name) else concat(designator,' ',abbreviation,' ',name) end")
  @Setter(AccessLevel.NONE)
  private String displayName;

  @Formula("length(designator)")
  @Setter(AccessLevel.NONE)
  private String designatorLength;

  protected <T extends Aspect> Optional<T> findAspectByDomain(Set<T> source, Domain domain) {
    return source.stream().filter(aspect -> aspect.getDomain().equals(domain)).findFirst();
  }

  @Override
  public Optional<String> findSubType(Domain domain) {
    return findAspectByDomain(subTypeAspects, domain).map(SubTypeAspect::getSubType);
  }

  @Override
  public Optional<String> findStatus(Domain domain) {
    return findAspectByDomain(subTypeAspects, domain).map(SubTypeAspect::getStatus);
  }

  @Override
  public void setStatus(String status, Domain domain) {
    findAspectByDomain(subTypeAspects, domain).orElseThrow().setStatus(status);
  }

  @Override
  public void addLink(CustomLink customLink) {
    if (findLink(customLink.getType(), customLink.getTarget(), customLink.getDomain())
        .isPresent()) {
      throw new EntityAlreadyExistsException(
          "Link with type '%s' and target ID %s already exists"
              .formatted(customLink.getType(), customLink.getTarget().getIdAsString()));
    }
    addToLinks(customLink);
  }

  @Override
  public void transferToDomain(Domain oldDomain, Domain newDomain) {
    requireAssociationWithDomain(oldDomain);
    if (domains.contains(newDomain)) {
      throw new EntityAlreadyExistsException(
          "%s %s is already associated with domain %s"
              .formatted(getModelType(), getIdAsString(), newDomain.getIdAsString()));
    }
    domains.remove(oldDomain);
    domains.add(newDomain);
    findAspectByDomain(subTypeAspects, oldDomain).ifPresent(a -> a.setDomain(newDomain));
    findAspectByDomain(decisionResultsAspects, oldDomain).ifPresent(a -> a.setDomain(newDomain));
    getCustomAspects(oldDomain).forEach(ca -> ca.setDomain(newDomain));
    getLinks(oldDomain).forEach(cl -> cl.setDomain(newDomain));
  }

  @Override
  public void associateWithDomain(@NonNull Domain domain, String subType, String status) {
    if (isAssociatedWithDomain(domain)) {
      throw new EntityAlreadyExistsException(
          "%s %s is already associated with domain %s"
              .formatted(getModelType(), getIdAsString(), domain.getIdAsString()));
    }
    domains.add(domain);
    removeAspect(subTypeAspects, domain);
    subTypeAspects.add(new SubTypeAspectData(domain, this, subType, status));

    // apply identically-defined custom aspects from old domains to new domain
    copyOf(customAspects).forEach(this::applyCustomAspect);
  }

  @Override
  public boolean removeFromDomains(Domain domain) {
    boolean removed = this.getDomains().remove(domain);
    if (removed) {
      getCustomAspects().removeIf(ca -> ca.getDomain().equals(domain));
      getLinks().removeIf(l -> l.getDomain().equals(domain));
      removeAspect(subTypeAspects, domain);
      removeAspect(decisionResultsAspects, domain);
    }
    return removed;
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
          if (aspect instanceof CustomAspectData customAspectData) {
            customAspectData.setOwner(this);
          }
        });
    this.customAspects.addAll(aCustomAspects);
  }

  @Override
  public Map<DecisionRef, DecisionResult> getDecisionResults(Domain domain) {
    return findAspectByDomain(decisionResultsAspects, domain)
        .map(DecisionResultsAspectData::getResults)
        .orElse(Map.of());
  }

  @Override
  public boolean setDecisionResults(Map<DecisionRef, DecisionResult> results, Domain domain) {
    if (!results.equals(getDecisionResults(domain))) {
      removeAspect(decisionResultsAspects, domain);
      decisionResultsAspects.add(new DecisionResultsAspectData(domain, this, results));
      return true;
    }
    return false;
  }

  @Override
  public boolean removeLink(CustomLink link) {
    forceUpdate();
    link.setSource(null);
    return this.links.remove(link);
  }

  public boolean removeCustomAspect(CustomAspect customAspect) {
    requireAssociationWithDomain(customAspect.getDomain());
    return getDomainsContainingSameCustomAspectDefinition(customAspect).stream()
        .map(d -> removeCustomAspectInIndividualDomain(customAspect.getType(), d))
        .toList()
        .contains(true);
  }

  @Override
  public void apply(CatalogItem catalogItem) {
    if (catalogItem.getDomain() instanceof Domain domain) {
      associateWithDomain(domain, catalogItem.getSubType(), catalogItem.getStatus());
      catalogItem.getCustomAspects().entrySet().stream()
          .map(e -> new CustomAspectData(e.getKey(), e.getValue(), domain))
          .forEach(this::applyCustomAspect);
      appliedCatalogItems.add(catalogItem);
    } else {
      throw new IllegalArgumentException("Cannot apply a catalog item from a domain template.");
    }
  }

  @Transient
  @Override
  public String getDisplayName() {
    return displayName;
  }

  @Transient
  @Override
  public Set<Domain> getDomainTemplates() {
    return new HashSet<>(domains);
  }

  @Override
  public Set<Domain> getAssociatedDomains() {
    return subTypeAspects.stream().map(SubTypeAspect::getDomain).collect(Collectors.toSet());
  }

  @Override
  public boolean applyCustomAspect(CustomAspect customAspect) {
    requireAssociationWithDomain(customAspect.getDomain());
    return getDomainsContainingSameCustomAspectDefinition(customAspect).stream()
        .map(
            d ->
                applyCustomAspectInIndividualDomain(
                    customAspect.getType(), customAspect.getAttributes(), d))
        .toList()
        .contains(true);
  }

  @Override
  public boolean applyLink(CustomLink newLink) {
    return findLink(newLink.getType(), newLink.getTarget(), newLink.getDomain())
        // TODO VEO-2086 implement and use CustomLink::apply(CustomLink)
        .map(oldLink -> oldLink.setAttributes(newLink.getAttributes()))
        .orElseGet(() -> addToLinks(newLink));
  }

  /**
   * @return all associated domains that have a custom aspect definition that is identical to how
   *     given custom aspect in its domain
   */
  private Set<Domain> getDomainsContainingSameCustomAspectDefinition(CustomAspect ca) {
    return ca.getDomain()
        .findCustomAspectDefinition(getModelType(), ca.getType())
        .map(
            definition ->
                getAssociatedDomains().stream()
                    .filter(
                        d ->
                            d.containsCustomAspectDefinition(
                                getModelType(), ca.getType(), definition))
                    .collect(Collectors.toSet()))
        // TODO VEO-2011 throw an exception if the custom aspect type is not defined in the target
        // domain. This workaround here (applying the custom aspect to the target domain anyway) is
        // necessary, because profile elements are persisted as JSON and not migrated when element
        // type definitions change. When the DTOs are deserialized from the profile JSON from the
        // DB they are mapped to entities and this method may be called for a custom aspect that is
        // no longer defined in the target domain (because the custom aspect definition has been
        // removed from the target domain).
        .orElse(Set.of(ca.getDomain()));
  }

  /**
   * Applies given custom aspect values only to the target domain, without checking custom aspect
   * definitions.
   *
   * @return {@code true} if any values were changed, otherwise {@code false}
   */
  private boolean applyCustomAspectInIndividualDomain(
      @NotNull String type, @NotNull Map<String, Object> attributes, @NotNull Domain domain) {
    return findCustomAspect(domain, type)
        // TODO VEO-2086 implement and use CustomAspect::apply(CustomAspect)
        .map(ca -> ca.setAttributes(attributes))
        .orElseGet(() -> addToCustomAspects(new CustomAspectData(type, attributes, domain)));
  }

  /**
   * Removes given custom aspect only from given domain, without checking custom aspect definitions.
   *
   * @return {@code true} if a custom aspect was found and removed, otherwise {@code false}
   */
  private boolean removeCustomAspectInIndividualDomain(
      @NotNull String type, @NotNull Domain domain) {
    return findCustomAspect(domain, type)
        .map(
            customAspect -> {
              if (customAspect instanceof CustomAspectData propertiesData) {
                propertiesData.setOwner(null);
              }
              this.customAspects.remove(customAspect);
              return true;
            })
        .orElse(false);
  }

  private Optional<CustomLink> findLink(String type, Element target, Domain domain) {
    return links.stream()
        .filter(
            l ->
                // TODO VEO-2086 implement and use CustomLink::matches(CustomLink)
                l.getDomain().equals(domain)
                    && l.getType().equals(type)
                    && l.getTarget().equals(target))
        .findFirst();
  }

  /**
   * Add the given CustomLink to the collection of links. Manages the association between Element
   * and CustomLink.
   *
   * @return true if the link was successfully added
   */
  private boolean addToLinks(CustomLink aCustomLink) {
    aCustomLink.setSource(this);
    return this.links.add(aCustomLink);
  }

  private Optional<CustomAspect> findCustomAspect(Domain domain, String type) {
    // TODO VEO-2086 implement and use CustomLink::matches(CustomLink)
    return getCustomAspects(domain).stream().filter(ca -> ca.getType().equals(type)).findFirst();
  }

  /**
   * Add the given {@link CustomAspect} to the collection customAspects.
   *
   * @return true if added
   */
  private boolean addToCustomAspects(CustomAspect aCustomAspect) {
    if (aCustomAspect instanceof CustomAspectData customAspectData) {
      customAspectData.setOwner(this);
    }
    return this.customAspects.add(aCustomAspect);
  }

  private void removeAspect(Set<? extends Aspect> aspects, Domain domain) {
    aspects.removeIf(a -> a.getDomain().equals(domain));
  }

  private void requireAssociationWithDomain(Domain domain) {
    if (!isAssociatedWithDomain(domain)) {
      throw new UnprocessableDataException(
          "%s %s is not associated with domain %s"
              .formatted(getModelType(), getIdAsString(), domain.getIdAsString()));
    }
  }

  /**
   * Convince JPA that this entity has been changed. This may be necessary when making changes to a
   * collection.
   */
  private void forceUpdate() {
    this.setUpdatedAt(Instant.now());
  }
}
