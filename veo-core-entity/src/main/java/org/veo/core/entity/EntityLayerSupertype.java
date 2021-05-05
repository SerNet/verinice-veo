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

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.veo.core.entity.format.AbbreviationPlaceholder;
import org.veo.core.entity.format.DisplayNameFormat;
import org.veo.core.entity.format.NamePlaceholder;
import org.veo.core.entity.format.OwnerPlaceholder;
import org.veo.core.entity.specification.ClientBoundaryViolationException;
import org.veo.core.entity.specification.EntitySpecifications;

/**
 * The abstract base model class. Used to prevent duplicating common methods in
 * model layer objects.
 */
public interface EntityLayerSupertype extends Nameable, ModelObject, ClientOwned, Catalogable {

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

    Optional<String> getSubType(Domain domain);

    void setSubType(Domain domain, String subType);

    /**
     * Add the given CustomProperties to the collection customAspects.
     *
     * @return true if added
     */
    boolean addToCustomAspects(CustomProperties aCustomProperties);

    /**
     * Remove the given CustomProperties from the collection customAspects.
     *
     * @return true if removed
     */
    boolean removeFromCustomAspects(CustomProperties aCustomProperties);

    Set<CustomProperties> getCustomAspects();

    void setCustomAspects(Set<CustomProperties> aCustomAspects);

    /**
     * @throws ClientBoundaryViolationException
     *             if the passed client is not equal to the client in the unit to
     *             which the entity belongs
     */
    default void checkSameClient(Client client) {
        Objects.requireNonNull(client, "client must not be null");
        Client thisEntitysClient = null;
        ElementOwner thisEntitysOwner = Objects.requireNonNull(getOwner(),
                                                               "No owner set for " + this);
        thisEntitysClient = Objects.requireNonNull(thisEntitysOwner.getClient(),
                                                   "No client set for " + thisEntitysOwner
                                                           + " might be part of a domain template");
        if (!(EntitySpecifications.hasSameClient(client)
                                  .isSatisfiedBy(thisEntitysClient))) {
            throw new ClientBoundaryViolationException("The client boundary would be "
                    + "violated by the attempted operation on element: " + toString()
                    + " from client " + client.toString());
        }
    }

    @Override
    default String getDisplayName() {
        // TODO VEO-284 Use configurable format template & placeholders,
        // optimize owner
        // retrieval performance.
        if (getOwner().isUnitType()) {
            var format = new DisplayNameFormat<>("%s - %s (%s)",
                    List.of(new AbbreviationPlaceholder<>(), new NamePlaceholder<>(),
                            new OwnerPlaceholder(new NamePlaceholder<>())));
            return format.render(this);
        } else {
            var format = new DisplayNameFormat<>("%s - %s",
                    List.of(new AbbreviationPlaceholder<>(), new NamePlaceholder<>()));
            return format.render(this);
        }

    }
}
