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
package org.veo.core.usecase;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import org.veo.core.entity.CatalogItem;
import org.veo.core.entity.Client;
import org.veo.core.entity.CustomLink;
import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainTemplate;
import org.veo.core.entity.Key;
import org.veo.core.entity.TailoringReference;
import org.veo.core.entity.TailoringReferenceType;
import org.veo.core.entity.exception.ModelConsistencyException;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.entity.specification.ClientBoundaryViolationException;
import org.veo.core.repository.ClientRepository;

/**
 * A collection of methods used by use cases.
 */
public final class UseCaseTools {

    private UseCaseTools() {
        super();
    }

    /**
     * Predicate to filter {@link TailoringReferenceType#COPY} or
     * {@link TailoringReferenceType#COPY_ALWAYS}.
     */
    public static final Predicate<? super TailoringReference> IS_COPY_PREDICATE = r -> r.getReferenceType() == TailoringReferenceType.COPY
            || r.getReferenceType() == TailoringReferenceType.COPY_ALWAYS;

    /**
     * Predicate to filter {@link TailoringReferenceType#LINK}
     */
    public static final Predicate<? super TailoringReference> IS_LINK_PREDICATE = r -> r.getReferenceType() == TailoringReferenceType.LINK;

    public static final Comparator<? super TailoringReference> BY_CATALOGITEM_ELEMENT = (c1,
            c2) -> c1.getCatalogItem()
                     .getElement()
                     .getId()
                     .uuidValue()
                     .compareTo(c2.getCatalogItem()
                                  .getElement()
                                  .getId()
                                  .uuidValue());

    /**
     * Orders the {@link TailoringReference} for applying.
     */
    public static final Comparator<? super TailoringReference> BY_EXECUTION = Comparator.comparing(TailoringReference::getReferenceType)
                                                                                        .thenComparing(BY_CATALOGITEM_ELEMENT);

    public static final Comparator<? super CustomLink> BY_LINK_TARGET = (c1, c2) -> c1.getTarget()
                                                                                      .getId()
                                                                                      .uuidValue()
                                                                                      .compareTo(c2.getTarget()
                                                                                                   .getId()
                                                                                                   .uuidValue());

    /**
     * Orders the string nullsafe by compareTo.
     */
    public static final Comparator<? super String> BY_STRING_NULL_SAFE = (s1,
            s2) -> StringUtils.compare(s1, s2, true);

    /**
     * Orders the links for applying.
     */
    public static final Comparator<? super CustomLink> BY_LINK_EXECUTION = Comparator.comparing(CustomLink::getType,
                                                                                                BY_STRING_NULL_SAFE)
                                                                                     .thenComparing(BY_LINK_TARGET);

    public static final Comparator<? super CatalogItem> BY_CATALOGITEMS = (ci1, ci2) -> ci1.getId()
                                                                                           .uuidValue()
                                                                                           .compareTo(ci2.getId()
                                                                                                         .uuidValue());

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
            throw new ClientBoundaryViolationException(domaintemplate, client);
        }
    }

    /**
     * Includes itself together with
     * {@link UseCaseTools#getElementsToCreate(CatalogItem)}. This list is ordered.
     * The item itself is at the first position.
     */
    public static List<CatalogItem> getAllElementsToCreate(CatalogItem catalogItem) {
        List<CatalogItem> allElementsToCreate = getElementsToCreate(catalogItem).stream()
                                                                                .sorted(BY_CATALOGITEMS)
                                                                                .collect(Collectors.toList());
        allElementsToCreate.add(0, catalogItem);
        return allElementsToCreate;
    }

    /**
     * Return the set additional elements to create. These elements are defined by
     * {@link TailoringReference} of type {@link TailoringReferenceType#COPY} or
     * {@link TailoringReferenceType#COPY_ALWAYS}.
     */
    public static Set<CatalogItem> getElementsToCreate(CatalogItem catalogItem) {
        Set<CatalogItem> elementsToCreate = new HashSet<>();
        catalogItem.getTailoringReferences()
                   .stream()
                   .filter(UseCaseTools.IS_COPY_PREDICATE)
                   .forEach(r -> addElementsToCopy(r, elementsToCreate));
        return elementsToCreate;
    }

    private static void addElementsToCopy(TailoringReference reference, Set<CatalogItem> set) {
        set.add(reference.getCatalogItem());
        reference.getCatalogItem()
                 .getTailoringReferences()
                 .stream()
                 .filter(UseCaseTools.IS_COPY_PREDICATE)
                 .forEach(rr -> addElementsToCopy(rr, set));
    }
}
