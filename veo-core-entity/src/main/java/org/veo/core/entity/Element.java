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

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.veo.core.entity.aspects.SubTypeAspect;
import org.veo.core.entity.specification.ClientBoundaryViolationException;
import org.veo.core.entity.specification.EntitySpecifications;

/**
 * Base type for entities that belong to a unit (a.k.a domain objects). Element
 * instances may be assigned to a set of domains and may hold aspects, custom
 * aspects & links within certain domains. They can also be of a domain-specific
 * sub type.
 */
public interface Element
        extends Nameable, Identifiable, ClientOwned, Designated, Versioned, Displayable {

    /**
     * Can be null when the owner is a catalogitem owned by a domain template.
     */
    default Optional<Client> getOwningClient() {
        return Optional.ofNullable(getOwner())
                       .map(ElementOwner::getClient);
    }

    /**
     * Add the given Domain to the collection domains.
     *
     * @return true if added
     */
    boolean addToDomains(Domain aDomain);

    /**
     * Remove the given Domain from the collection domains.
     *
     * @return true if removed
     */
    boolean removeFromDomains(Domain aDomain);

    Set<Domain> getDomains();

    void setDomains(Set<Domain> aDomains);

    /**
     * Add the given CustomLink to the collection links. Adding will set the source
     * to this.
     *
     * @return true if added
     */
    boolean addToLinks(CustomLink aCustomLink);

    /**
     * Remove the given CustomLink from the collection links. Removing will set the
     * source to null.
     *
     * @return true if removed
     */
    boolean removeFromLinks(CustomLink aCustomLink);

    Set<CustomLink> getLinks();

    void setLinks(Set<CustomLink> aLinks);

    Optional<String> getSubType(DomainTemplate domain);

    Optional<String> getStatus(DomainTemplate domain);

    void setSubType(DomainTemplate domain, String subType, String status);

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
     * @throws ClientBoundaryViolationException
     *             if the passed client is not equal to the client in the unit to
     *             which the entity belongs
     */
    default void checkSameClient(Element element) {
        checkSameClient(element.getOwnerOrContainingCatalogItem()
                               .getClient());
    }

    /**
     * @throws ClientBoundaryViolationException
     *             if the passed client is not equal to the client in the unit to
     *             which the entity belongs
     */
    default void checkSameClient(Client client) {
        Objects.requireNonNull(client, "client must not be null");
        Client thisEntitysClient = null;
        ElementOwner thisEntitysOwner = Objects.requireNonNull(getOwnerOrContainingCatalogItem(),
                                                               "No owner or containing catalog item set for "
                                                                       + this);
        thisEntitysClient = Objects.requireNonNull(thisEntitysOwner.getClient(),
                                                   "No client set for " + thisEntitysOwner
                                                           + " might be part of a domain template");
        if (!(EntitySpecifications.hasSameClient(client)
                                  .isSatisfiedBy(thisEntitysClient))) {
            throw new ClientBoundaryViolationException(this, client);
        }
    }

    Set<SubTypeAspect> getSubTypeAspects();

    /**
     * Stores the references of the applied catalog items.
     */
    // TODO VEO-889: Should this be unique in one domain/template? Should an object
    // exist in two different version of the same domainTemplate?
    Set<CatalogItem> getAppliedCatalogItems();

    void setAppliedCatalogItems(Set<CatalogItem> aCatalogitems);

    Unit getOwner();

    void setOwner(Unit unit);

    default ElementOwner getOwnerOrContainingCatalogItem() {
        return Optional.<ElementOwner> ofNullable(getOwner())
                       .orElse(getContainingCatalogItem());
    }

    default void setOwnerOrContainingCatalogItem(ElementOwner owner) {
        if (owner instanceof Unit) {
            this.setOwner((Unit) owner);
            this.setContainingCatalogItem(null);
        } else {
            this.setOwner(null);
            this.setContainingCatalogItem((CatalogItem) owner);
        }
    }

    CatalogItem getContainingCatalogItem();

    void setContainingCatalogItem(CatalogItem containigCatalogItem);
}
