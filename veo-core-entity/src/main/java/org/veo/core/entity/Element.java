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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.validation.constraints.NotNull;

import org.veo.core.entity.aspects.ElementDomainAssociation;
import org.veo.core.entity.decision.DecisionRef;
import org.veo.core.entity.decision.DecisionResult;
import org.veo.core.entity.domainmigration.DomainSpecificValueLocation;
import org.veo.core.entity.specification.ClientBoundaryViolationException;

/**
 * Base type for entities that belong to a unit (a.k.a domain objects). Element instances may be
 * assigned to a set of domains and may hold aspects, custom aspects & links within certain domains.
 * They can also be of a domain-specific sub type.
 */
public interface Element
    extends Nameable, Identifiable, ClientOwned, Designated, Versioned, Displayable {

  @Override
  default Optional<Client> getOwningClient() {
    return Optional.ofNullable(getOwner()).map(Unit::getClient);
  }

  /**
   * Remove the given Domain from the collection domains.
   *
   * @return true if removed
   */
  boolean removeFromDomains(Domain domain);

  /**
   * @return all domains that this element is associated with
   */
  default Set<Domain> getDomains() {
    return getDomainAssociations().stream()
        .map(ElementDomainAssociation::getDomain)
        .collect(Collectors.toSet());
  }

  /**
   * Removes given {@link CustomLink} from this element.
   *
   * @return {@code true} if removed
   */
  boolean removeLink(CustomLink aCustomLink);

  Set<CustomLink> getLinks();

  void setLinks(Set<CustomLink> aLinks);

  Optional<String> findSubType(Domain domain);

  Optional<String> findStatus(Domain domain);

  default String getSubType(Domain domain) {
    return findSubType(domain)
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    "%s %s not associated with domain %s"
                        .formatted(getModelType(), getIdAsString(), domain.getIdAsString())));
  }

  default String getStatus(Domain domain) {
    return findStatus(domain)
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    "%s %s not associated with domain %s"
                        .formatted(getModelType(), getIdAsString(), domain.getIdAsString())));
  }

  /**
   * Associate this element with an additional domain. This makes the element visible to users in
   * the context of given domain and allows domain-specific information such as custom aspects or
   * links to be added for that domain. When associating an element with a domain, existing custom
   * aspects from other domains that are identically defined in the new domain are copied to the new
   * domain.
   */
  void associateWithDomain(Domain domain, String subType, String status);

  /**
   * Removes given {@link CustomAspect} from this element.
   *
   * @return {@code true} if removed
   */
  boolean removeCustomAspect(CustomAspect customAspect);

  Set<CustomAspect> getCustomAspects();

  /**
   * @throws ClientBoundaryViolationException if the passed client is not equal to the client in the
   *     unit to which the entity belongs
   */
  default void checkSameClient(Element element) {
    element.checkSameClient(getOwningClient().get());
  }

  Set<ElementDomainAssociation> getDomainAssociations();

  // TODO VEO-889: Should this be unique in one domain/template? Should an object
  // exist in two different version of the same domainTemplate?
  /**
   * Catalog items that have been applied to this element. This is a many-to-many relationship,
   * because an element may have been created from a catalog item in one domain with additional
   * catalog items from other domains applied to it.
   *
   * <p>There may only be one catalog item per domain referenced in this set.
   */
  default Set<CatalogItem> getAppliedCatalogItems() {
    return getDomainAssociations().stream()
        .map(ElementDomainAssociation::getAppliedCatalogItem)
        .filter(Predicate.not(Objects::isNull))
        .collect(Collectors.toUnmodifiableSet());
  }

  Optional<CatalogItem> findAppliedCatalogItem(Domain domain);

  void setAppliedCatalogItem(Domain newDomain, CatalogItem item);

  /** Applies the properties of the template item to the element. */
  void apply(TemplateItem<?, ?> item);

  @NotNull
  Unit getOwner();

  void setOwner(Unit unit);

  Set<Scope> getScopes();

  /** Detach this element from related elements to prepare for deletion. */
  void remove();

  Map<DecisionRef, DecisionResult> getDecisionResults(Domain domain);

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

  default Set<CustomAspect> getCustomAspects(Domain domain) {
    return getCustomAspects().stream().filter(ca -> ca.getDomain().equals(domain)).collect(toSet());
  }

  default Optional<CustomAspect> findCustomAspect(Domain domain, String type) {
    // TODO VEO-2086 implement and use CustomLink::matches(CustomLink)
    return getCustomAspects(domain).stream().filter(ca -> ca.getType().equals(type)).findFirst();
  }

  default Set<CustomLink> getLinks(Domain domain) {
    return getLinks().stream().filter(l -> l.getDomain().equals(domain)).collect(toSet());
  }

  default boolean isAssociatedWithDomain(Domain domain) {
    return findSubType(domain).isPresent();
  }

  /**
   * Applies given custom aspect, by either applying its attributes to a corresponding existing
   * custom aspect or by adding it as a new custom aspect (if no corresponding custom aspect
   * exists). The change is propagated to all domains that have a definition for the given custom
   * aspect type that is identical to the definition in the target domain.
   *
   * @return {@code true} if anything has changed on the element
   */
  boolean applyCustomAspect(CustomAspect customAspect);

  /**
   * Applies given link, by either applying its attributes to a corresponding existing link or by
   * adding it as a new link (if no corresponding link exists).
   *
   * @return {@code true} if anything has changed on the element
   */
  boolean applyLink(CustomLink customLink);

  /**
   * @throws org.veo.core.entity.exception.EntityAlreadyExistsException if an equivalent link (a
   *     link with the same domain, type & target) already exists on this element.
   */
  void addLink(CustomLink customLink);

  void setStatus(String status, Domain domain);

  /**
   * Applies the properties of this element to the catalog item.
   *
   * <p>If the element does not reference a catalog item in given domain yet, a new catalog item is
   * created, and a reference to the new item is added to this element (see {@link
   * Element#getAppliedCatalogItems()})
   */
  CatalogItem toCatalogItem(Domain domain);

  ProfileItem toProfileItem(Profile profile);

  void copyDomainData(
      Domain oldDomain,
      Domain newDomain,
      Collection<DomainSpecificValueLocation> excludedDefinitions);

  void applyCustomAspectAttribute(Domain domain, String caType, String attribute, Object value);

  default ElementType getType() {
    return ElementType.fromModelInterface(getModelInterface());
  }
}
