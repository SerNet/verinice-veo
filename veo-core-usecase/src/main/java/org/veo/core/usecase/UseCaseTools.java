/*******************************************************************************
 * Copyright (c) 2021 Urs Zeidler.
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.veo.core.usecase;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

import org.veo.core.entity.Catalog;
import org.veo.core.entity.CatalogItem;
import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainTemplate;
import org.veo.core.entity.Key;
import org.veo.core.entity.exception.ModelConsistencyException;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.entity.specification.ClientBoundaryViolationException;
import org.veo.core.repository.ClientRepository;

/**
 * A collection of methods used by use cases.
 */
public class UseCaseTools {
    public static final Predicate<? super Object> TRUE_PREDICATE = c -> true;
    public static final Predicate<? super Domain> DOMAIN_IS_ACTIVE_PREDICATE = d -> d.isActive();

    public static Predicate<? super Domain> getDomainIdPredicate(Optional<Key<UUID>> domainId) {
        if (domainId.isPresent()) {
            return d -> domainId.get()
                                .equals(d.getId());
        } else
            return TRUE_PREDICATE;
    }

    public static Predicate<? super Catalog> getCatalogIdPredicate(Key<UUID> catalogId) {
        return c -> c.getId()
                     .equals(catalogId);

    }

    public static Predicate<? super CatalogItem> getNamespacePredicate(Optional<String> namespace) {
        if (namespace.isPresent()) {
            return ci -> namespace.get()
                                  .equals(ci.getNamespace());
        }
        return TRUE_PREDICATE;
    }

    /**
     * Check if the client exists.
     *
     * @throws NotFoundException
     */
    public static Client checkClientExists(Key<UUID> clientId, ClientRepository clientRepository) {
        Client client = clientRepository.findById(clientId)
                                        .orElseThrow(() -> new NotFoundException(
                                                "Invalid client ID"));
        return client;
    }

    /**
     * Checks if the given domain is owned by the client.
     *
     * @throws IllegalArgumentException
     *             when used with a Domaintemplate instance, as Domaintemplate can
     *             not be owned by a client.
     * @throws ModelConsistencyException
     *             when the domain is not owned by the client.
     */
    public static void checkDomainBelongsToClient(Client client, DomainTemplate domaintemplate) {
        if (!Domain.class.isAssignableFrom(domaintemplate.getClass())) {
            throw new IllegalArgumentException("A DomainTemplate never belongs to a client");
        }
        if (!client.getDomains()
                   .contains(domaintemplate)) {
            throw new ClientBoundaryViolationException(
                    "This object is not accessable from this client.");
        }
    }

}
