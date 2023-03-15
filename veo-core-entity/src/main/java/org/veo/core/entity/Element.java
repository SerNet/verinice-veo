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
package org.veo.core.entity;

import static java.util.stream.Collectors.toSet;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.veo.core.entity.aspects.SubTypeAspect;
import org.veo.core.entity.decision.DecisionRef;
import org.veo.core.entity.decision.DecisionResult;
import org.veo.core.entity.specification.ClientBoundaryViolationException;
import org.veo.core.entity.specification.EntitySpecifications;

/**
 * Base type for entities that belong to a unit (a.k.a domain objects). Element instances may be
 * assigned to a set of domains and may hold aspects, custom aspects & links within certain domains.
 * They can also be of a domain-specific sub type.
 */
public interface Element
    extends Nameable, Identifiable, ClientOwned, Designated, Versioned, Displayable {

  int ELEMENT_TYPE_MAX_LENGTH = Constraints.DEFAULT_CONSTANT_MAX_LENGTH;

  /** Can be null when the owner is a catalogitem owned by a domain template. */
  default Optional<Client> getOwningClient() {
    return Optional.ofNullable(getOwner()).map(ElementOwner::getClient);
  }

  /**
   * Remove the given Domain from the collection domains.
   *
   * @return true if removed
   */
  boolean removeFromDomains(Domain domain);

  Set<Domain> getDomains();

  default void transferToDomain(Domain oldDomain, Domain newDomain) {
    if (!getDomains().contains(oldDomain)) {
      throw new IllegalArgumentException(this + " is not a member of " + oldDomain);
    }
    if (getDomains().contains(newDomain)) {
      throw new IllegalArgumentException(this + " is already a member of " + newDomain);
    }
    associateWithDomain(newDomain, getSubType(oldDomain), getStatus(oldDomain));
    getCustomAspects().forEach(ca -> ca.setDomain(newDomain));
    getLinks().forEach(cl -> cl.setDomain(newDomain));
    removeFromDomains(oldDomain);
  }

  /**
   * Add the given CustomLink to the collection links. Adding will set the source to this.
   *
   * @return true if added
   */
  boolean addToLinks(CustomLink aCustomLink);

  /**
   * Remove the given CustomLink from the collection links. Removing will set the source to null.
   *
   * @return true if removed
   */
  boolean removeFromLinks(CustomLink aCustomLink);

  Set<CustomLink> getLinks();

  void setLinks(Set<CustomLink> aLinks);

  Optional<String> findSubType(DomainBase domain);

  Optional<String> findStatus(DomainBase domain);

  default String getSubType(DomainBase domain) {
    return findSubType(domain)
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    "%s %s not associated with domain %s"
                        .formatted(getModelType(), getIdAsString(), domain.getIdAsString())));
  }

  default String getStatus(DomainBase domain) {
    return findStatus(domain)
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    "%s %s not associated with domain %s"
                        .formatted(getModelType(), getIdAsString(), domain.getIdAsString())));
  }

  boolean associateWithDomain(DomainBase domain, String subType, String status);

  /**
   * Add the given {@link CustomAspect} to the collection customAspects.
   *
   * @return true if added
   */
  boolean addToCustomAspects(CustomAspect aCustomAspect);

  /**
   * Remove the given {@link CustomAspect} from the collection customAspects.
   *
   * @return true if removed
   */
  boolean removeFromCustomAspects(CustomAspect aCustomAspect);

  Set<CustomAspect> getCustomAspects();

  void setCustomAspects(Set<CustomAspect> aCustomAspects);

  /**
   * @throws ClientBoundaryViolationException if the passed client is not equal to the client in the
   *     unit to which the entity belongs
   */
  default void checkSameClient(Element element) {
    checkSameClient(element.getOwnerOrContainingCatalogItem().getClient());
  }

  /**
   * @throws ClientBoundaryViolationException if the passed client is not equal to the client in the
   *     unit to which the entity belongs
   */
  default void checkSameClient(Client client) {
    Objects.requireNonNull(client, "client must not be null");
    Client thisEntitysClient;
    ElementOwner thisEntitysOwner =
        Objects.requireNonNull(
            getOwnerOrContainingCatalogItem(),
            "No owner or containing catalog item set for " + this);
    thisEntitysClient =
        Objects.requireNonNull(
            thisEntitysOwner.getClient(),
            "No client set for " + thisEntitysOwner + " might be part of a domain template");
    if (!(EntitySpecifications.hasSameClient(client).isSatisfiedBy(thisEntitysClient))) {
      throw new ClientBoundaryViolationException(this, client);
    }
  }

  Set<SubTypeAspect> getSubTypeAspects();

  /** Stores the references of the applied catalog items. */
  // TODO VEO-889: Should this be unique in one domain/template? Should an object
  // exist in two different version of the same domainTemplate?
  Set<CatalogItem> getAppliedCatalogItems();

  void setAppliedCatalogItems(Set<CatalogItem> aCatalogitems);

  Unit getOwner();

  void setOwner(Unit unit);

  default ElementOwner getOwnerOrContainingCatalogItem() {
    return Optional.<ElementOwner>ofNullable(getOwner()).orElse(getContainingCatalogItem());
  }

  default void setOwnerOrContainingCatalogItem(ElementOwner owner) {
    if (owner instanceof Unit unit) {
      this.setOwner(unit);
      this.setContainingCatalogItem(null);
    } else {
      this.setOwner(null);
      this.setContainingCatalogItem((CatalogItem) owner);
    }
  }

  CatalogItem getContainingCatalogItem();

  void setContainingCatalogItem(CatalogItem containigCatalogItem);

  Set<Scope> getScopes();

  /** Detach this element from related elements to prepare for deletion. */
  void remove();

  Map<DecisionRef, DecisionResult> getDecisionResults(DomainBase domain);

  /**
   * Update all decision results in given domain.
   *
   * @return {@code true} if new results differ from previous values, otherwise {@code false}
   */
  boolean setDecisionResults(Map<DecisionRef, DecisionResult> decisionResults, Domain domain);

  /**
   * Update the result of given decision in given domain.
   *
   * @return {@code true} if new result differs from previous value, otherwise {@code false}
   */
  default boolean setDecisionResult(DecisionRef decisionRef, DecisionResult result, Domain domain) {
    var domainResults = new HashMap<>(getDecisionResults(domain));
    domainResults.put(decisionRef, result);
    return setDecisionResults(domainResults, domain);
  }

  /**
   * Retrieve all domains or domain templates this element is associated with. If this is a catalog
   * item, the catalog's domain (template) is returned.
   */
  Set<DomainBase> getDomainTemplates();

  default Set<CustomAspect> getCustomAspects(Domain domain) {
    return getCustomAspects().stream().filter(ca -> ca.getDomain().equals(domain)).collect(toSet());
  }

  default Set<CustomLink> getLinks(Domain domain) {
    return getLinks().stream().filter(l -> l.getDomain().equals(domain)).collect(toSet());
  }

  default boolean isAssociatedWithDomain(DomainBase domain) {
    if (getContainingCatalogItem() != null) {
      return getContainingCatalogItem().getCatalog().getDomainTemplate().equals(domain);
    }
    return getDomains().contains(domain);
  }
}
