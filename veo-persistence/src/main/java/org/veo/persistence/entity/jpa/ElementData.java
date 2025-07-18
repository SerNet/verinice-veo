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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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
import org.hibernate.annotations.UuidGenerator;

import org.veo.core.entity.CatalogItem;
import org.veo.core.entity.CustomAspect;
import org.veo.core.entity.CustomLink;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;
import org.veo.core.entity.ElementType;
import org.veo.core.entity.Nameable;
import org.veo.core.entity.Profile;
import org.veo.core.entity.ProfileItem;
import org.veo.core.entity.Scope;
import org.veo.core.entity.TemplateItem;
import org.veo.core.entity.TemplateItemAspects;
import org.veo.core.entity.Unit;
import org.veo.core.entity.aspects.Aspect;
import org.veo.core.entity.aspects.ElementDomainAssociation;
import org.veo.core.entity.decision.DecisionRef;
import org.veo.core.entity.decision.DecisionResult;
import org.veo.core.entity.definitions.ElementTypeDefinition;
import org.veo.core.entity.definitions.LinkDefinition;
import org.veo.core.entity.domainmigration.CustomAspectAttribute;
import org.veo.core.entity.domainmigration.DomainSpecificValueLocation;
import org.veo.core.entity.exception.EntityAlreadyExistsException;
import org.veo.core.entity.exception.UnprocessableDataException;
import org.veo.persistence.entity.jpa.transformer.EntityDataFactory;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

@Entity(name = "element")
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@EntityListeners({VersionedEntityListener.class})
@Data
public abstract class ElementData extends IdentifiableVersionedData implements Element {

  @Id
  @ToString.Include
  @GeneratedValue(generator = "UUID")
  @UuidGenerator
  @Column(name = "db_id")
  private UUID id;

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
      targetEntity = ElementDomainAssociationData.class,
      mappedBy = "owner",
      fetch = FetchType.LAZY)
  @Valid
  private Set<ElementDomainAssociation> domainAssociations = new HashSet<>();

  @Column(name = "decision_results_aspect")
  @OneToMany(
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      targetEntity = DecisionResultsAspectData.class,
      mappedBy = "owner",
      fetch = FetchType.LAZY)
  @Valid
  private final Set<DecisionResultsAspectData> decisionResultsAspects = new HashSet<>();

  @Override
  public void setOwner(Unit owner) {
    if (this.owner != null && !owner.equals(this.owner)) {
      throw new UnprocessableDataException("Elements cannot be moved between units");
    }
    this.owner = owner;
  }

  @ManyToOne(fetch = FetchType.LAZY, targetEntity = UnitData.class)
  @JoinColumn(name = "owner_id")
  private Unit owner;

  @Formula(
      "case when abbreviation is null then concat(designator,' ',name) else concat(designator,' ',abbreviation,' ',name) end")
  @Setter(AccessLevel.NONE)
  private String displayName;

  @Setter(value = AccessLevel.NONE)
  @Formula("dtype")
  private ElementType elementType;

  protected <T extends Aspect> Optional<T> findAspectByDomain(Set<T> source, Domain domain) {
    return source.stream().filter(aspect -> domain.equals(aspect.getDomain())).findFirst();
  }

  @Override
  public Optional<String> findSubType(Domain domain) {
    return findAspectByDomain(domainAssociations, domain).map(ElementDomainAssociation::getSubType);
  }

  @Override
  public Optional<String> findStatus(Domain domain) {
    return findAspectByDomain(domainAssociations, domain).map(ElementDomainAssociation::getStatus);
  }

  @Override
  public Optional<CatalogItem> findAppliedCatalogItem(Domain domain) {
    return findAspectByDomain(domainAssociations, domain)
        .map(ElementDomainAssociation::getAppliedCatalogItem);
  }

  @Override
  public void setStatus(String status, Domain domain) {
    findAspectByDomain(domainAssociations, domain).orElseThrow().setStatus(status);
  }

  @Override
  public void setAppliedCatalogItem(Domain domain, CatalogItem item) {
    requireAssociationWithDomain(domain).setAppliedCatalogItem(item);
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

  protected void removeAspectByDomain(Set<? extends AspectData> aspects, Domain domain) {
    aspects.removeIf(a -> a.getDomain().equals(domain));
  }

  @Override
  public void associateWithDomain(@NonNull Domain domain, String subType, String status) {
    if (isAssociatedWithDomain(domain)) {
      throw new EntityAlreadyExistsException(
          "%s %s is already associated with domain %s"
              .formatted(getModelType(), getIdAsString(), domain.getIdAsString()));
    }
    removeAspect(domainAssociations, domain);
    domainAssociations.add(new ElementDomainAssociationData(domain, this, subType, status));

    // apply identically-defined custom aspects from old domains to new domain
    copyOf(customAspects).forEach(this::applyCustomAspect);
  }

  @Override
  public void copyDomainData(
      Domain oldDomain,
      Domain newDomain,
      Collection<DomainSpecificValueLocation> excludedDefinitions) {

    var nETD = newDomain.getElementTypeDefinition(getType());
    var oETD = oldDomain.getElementTypeDefinition(getType());

    migrateCustomAspects(oldDomain, newDomain, excludedDefinitions);
    migrateCustomLinks(oldDomain, newDomain, nETD, oETD);

    findAppliedCatalogItem(oldDomain)
        .flatMap(oldItem -> newDomain.findCatalogItem(oldItem.getSymbolicId()))
        .ifPresent(item -> setAppliedCatalogItem(newDomain, item));
  }

  private void migrateCustomAspects(
      Domain oldDomain,
      Domain newDomain,
      Collection<DomainSpecificValueLocation> deprecatedDefinitions) {
    getCustomAspects(oldDomain)
        .forEach(
            ca -> {
              String caType = ca.getType();
              if (findCustomAspect(newDomain, caType)
                  .isEmpty()) { // Unaltered CAs have already been carried over during CA sync on
                // domain association
                var attributes =
                    ca.getAttributes().entrySet().stream()
                        .filter(e -> isIncluded(deprecatedDefinitions, ca, e))
                        .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
                if (!attributes.isEmpty()) {
                  applyCustomAspect(new CustomAspectData(caType, attributes, newDomain));
                }
              }
            });
  }

  private boolean isIncluded(
      Collection<DomainSpecificValueLocation> deprecatedDefinitions,
      CustomAspect ca,
      Entry<String, Object> e) {
    return !deprecatedDefinitions.contains(
        new CustomAspectAttribute(getType(), ca.getType(), e.getKey()));
  }

  private void migrateCustomLinks(
      Domain oldDomain,
      Domain newDomain,
      ElementTypeDefinition newElementTypeDefinition,
      ElementTypeDefinition oldElementTypeDefinition) {
    getLinks(oldDomain).stream()
        .forEach(
            cl -> {
              LinkDefinition newLD = newElementTypeDefinition.getLinks().get(cl.getType());
              LinkDefinition oldLD = oldElementTypeDefinition.getLinks().get(cl.getType());
              if (newLD != null) { // TODO: vernice-veo#3381
                if (isCompatible(newLD, oldLD)) {
                  CustomLink link = new CustomLinkData();
                  link.setDomain(newDomain);
                  link.setType(cl.getType());
                  link.setTarget(cl.getTarget());
                  link.setAttributes(cl.getAttributes());
                  addLink(link);
                }
              }
            });
  }

  private boolean isCompatible(LinkDefinition newDefinition, LinkDefinition oldDefinition) {
    return Objects.equals(newDefinition, oldDefinition);
  }

  @Override
  public boolean removeFromDomains(Domain domain) {
    boolean removed = this.getDomains().remove(domain);
    if (removed) {
      getCustomAspects().removeIf(ca -> ca.getDomain().equals(domain));
      getLinks().removeIf(l -> l.getDomain().equals(domain));
      removeAspect(domainAssociations, domain);
      removeAspect(decisionResultsAspects, domain);
    }
    return removed;
  }

  @Override
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

  @Override
  public boolean removeCustomAspect(CustomAspect customAspect) {
    requireAssociationWithDomain(customAspect.getDomain());
    return getDomainsContainingSameCustomAspectDefinition(customAspect).stream()
        .map(d -> removeCustomAspectInIndividualDomain(customAspect.getType(), d))
        .toList()
        .contains(true);
  }

  @Override
  public void apply(TemplateItem<?, ?> item) {
    var domain = item.requireDomainMembership();
    associateWithDomain(domain, item.getSubType(), item.getStatus());
    item.getCustomAspects().entrySet().stream()
        .map(e -> new CustomAspectData(e.getKey(), e.getValue(), domain))
        .forEach(this::applyCustomAspect);
    applyItemAspects(item.getAspects(), domain);
  }

  protected abstract void applyItemAspects(TemplateItemAspects itemAspects, Domain domain);

  @Transient
  @Override
  public String getDisplayName() {
    return displayName;
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

  @Override
  public CatalogItem toCatalogItem(Domain domain) {
    var item =
        findAppliedCatalogItem(domain)
            .orElseGet(() -> new EntityDataFactory().createCatalogItem(domain));
    toItem(domain, item);
    setAppliedCatalogItem(domain, item);
    return item;
  }

  @Override
  public ProfileItem toProfileItem(Profile profile) {
    ProfileItem item = new EntityDataFactory().createProfileItem(profile);
    toItem((Domain) profile.getOwner(), item);
    findAppliedCatalogItem((Domain) profile.getOwner()).ifPresent(item::setAppliedCatalogItem);
    return item;
  }

  @Override
  public CustomAspect findOrAddCustomAspect(Domain domain, String type) {
    return findCustomAspect(domain, type)
        .orElseGet(
            () -> {
              var ca = new CustomAspectData(type, new HashMap<>(), domain);
              applyCustomAspect(ca);
              return ca;
            });
  }

  private void toItem(Domain domain, TemplateItem<?, ?> item) {
    item.setName(getName());
    item.setAbbreviation(getAbbreviation());
    item.setDescription(getDescription());
    item.setElementType(getType());
    item.setStatus(getStatus(domain));
    item.setSubType(getSubType(domain));
    item.setCustomAspects(
        getCustomAspects(domain).stream()
            .collect(Collectors.toMap(CustomAspect::getType, CustomAspect::getAttributes)));
    item.setAspects(mapAspectsToItem(domain));
    if (item.getUpdatedAt() != null) {
      item.setUpdatedAt(Instant.now());
    }
  }

  protected TemplateItemAspects mapAspectsToItem(Domain domain) {
    return new TemplateItemAspects();
  }

  /**
   * @return all associated domains that have a custom aspect definition that is identical to how
   *     given custom aspect in its domain
   */
  private Set<Domain> getDomainsContainingSameCustomAspectDefinition(CustomAspect ca) {
    return ca.getDomain()
        .findCustomAspectDefinition(getType(), ca.getType())
        .map(
            definition ->
                getDomains().stream()
                    .filter(
                        d -> d.containsCustomAspectDefinition(getType(), ca.getType(), definition))
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

  private ElementDomainAssociation requireAssociationWithDomain(Domain domain) {
    return findAspectByDomain(domainAssociations, domain)
        .orElseThrow(
            () ->
                new UnprocessableDataException(
                    "%s %s is not associated with domain %s"
                        .formatted(getModelType(), getIdAsString(), domain.getIdAsString())));
  }

  /**
   * Convince JPA that this entity has been changed. This may be necessary when making changes to a
   * collection.
   */
  private void forceUpdate() {
    this.setUpdatedAt(Instant.now());
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
