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
package org.veo.core.usecase.catalogitem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.veo.core.entity.CatalogItem;
import org.veo.core.entity.Client;
import org.veo.core.entity.CustomLink;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;
import org.veo.core.entity.ExternalTailoringReference;
import org.veo.core.entity.Key;
import org.veo.core.entity.TailoringReference;
import org.veo.core.entity.TailoringReferenceType;
import org.veo.core.entity.TailoringReferenceTyped;
import org.veo.core.entity.Unit;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.entity.transform.EntityFactory;
import org.veo.core.entity.util.CustomLinkComparators;
import org.veo.core.entity.util.TailoringReferenceComparators;
import org.veo.core.repository.CatalogItemRepository;
import org.veo.core.repository.ElementRepository;
import org.veo.core.repository.UnitRepository;
import org.veo.core.service.CatalogItemService;
import org.veo.core.usecase.DesignatorService;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.UseCaseTools;
import org.veo.core.usecase.parameter.IncarnateCatalogItemDescription;
import org.veo.core.usecase.parameter.TailoringReferenceParameter;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

/**
 * Uses a list of {@link IncarnateCatalogItemDescription} to create items from a
 * catalog in a unit.
 */
@AllArgsConstructor
@Slf4j
public class ApplyIncarnationDescriptionUseCase implements
        TransactionalUseCase<ApplyIncarnationDescriptionUseCase.InputData, ApplyIncarnationDescriptionUseCase.OutputData> {
    private final UnitRepository unitRepository;
    private final CatalogItemRepository catalogItemRepository;
    private final org.veo.core.repository.RepositoryProvider repositoryProvider;
    private final DesignatorService designatorService;
    private final CatalogItemService catalogItemservice;
    private final EntityFactory factory;

    @Override
    public OutputData execute(InputData input) {
        log.info("ApplyIncarnationDescriptionUseCase: {}", input);
        Unit unit = unitRepository.findById(input.getContainerId())
                                  .orElseThrow(() -> new NotFoundException("Unit %s not found.",
                                          input.getContainerId()));
        Client authenticatedClient = input.authenticatedClient;
        unit.checkSameClient(authenticatedClient);

        Supplier<IncarnationResult> supplier = IncarnationResult::new;
        BiConsumer<IncarnationResult, ElementResult> consumer = (elementData, iElement) -> {
            elementData.elements.add(iElement.element);
            elementData.internalLinks.addAll(iElement.getInternalLinks());
        };
        BinaryOperator<IncarnationResult> operator = (source, data) -> {
            source.elements.addAll(data.elements);
            source.internalLinks.addAll(data.internalLinks);
            return source;
        };
        Function<IncarnationResult, List<Element>> function = elementData -> {
            processInternalLinks(elementData.internalLinks, elementData.elements);
            return elementData.elements;
        };
        List<Element> createdElements = input.getReferencesToApply()
                                             .stream()
                                             .map(ra -> {
                                                 CatalogItem catalogItem = catalogItemRepository.findById(ra.getItem()
                                                                                                            .getId())
                                                                                                .orElseThrow(() -> new NotFoundException(
                                                                                                        "CatalogItem not found %s",
                                                                                                        ra.getItem()
                                                                                                          .getId()));

                                                 Domain domain = (Domain) catalogItem.getCatalog()
                                                                                     .getDomainTemplate();
                                                 UseCaseTools.checkDomainBelongsToClient(input.getAuthenticatedClient(),
                                                                                         domain);
                                                 return createElementFromCatalogItem(unit,
                                                                                     authenticatedClient,
                                                                                     catalogItem,
                                                                                     domain,
                                                                                     ra.getReferences());
                                             })
                                             .collect(Collector.of(supplier, consumer, operator,
                                                                   function));
        log.info("ApplyIncarnationDescriptionUseCase elements created: {}", createdElements);
        return new ApplyIncarnationDescriptionUseCase.OutputData(createdElements);
    }

    /**
     * Incarnate a catalogItem, uses the
     * {@link CatalogItemService#createInstance(CatalogItem, Domain)} to create a
     * copy of the element. Set the customLinkTargets of this element to the given
     * referencesToApply. Assign the designator, save the element and create the
     * links in the opposite objects which are defined by the
     * {@link ExternalTailoringReference}.
     */
    private ElementResult createElementFromCatalogItem(Unit unit, Client authenticatedClient,
            CatalogItem catalogItem, Domain domain,
            List<TailoringReferenceParameter> referencesToApply) {
        validateItem(catalogItem, referencesToApply);
        Element entity = catalogItemservice.createInstance(catalogItem, domain);
        List<InternalResolveInfo> internalLinks = applyLinkTailoringReferences(entity,
                                                                               referencesToApply.stream()
                                                                                                .filter(TailoringReferenceTyped.IS_LINK_PREDICATE)
                                                                                                .collect(Collectors.toList()),
                                                                               catalogItem, domain);
        entity.setOwner(unit);
        designatorService.assignDesignator(entity, authenticatedClient);
        entity = saveElement(entity);
        applyExternalTailoringReferences(entity, domain, externalTailorReferences(catalogItem),
                                         externalTailorReferencesParameters(referencesToApply));
        return new ElementResult(entity, internalLinks);
    }

    /**
     * Apply the parameters from the incarnation description to the newly created
     * object. This method handles the {@link TailoringReferenceType#LINK} type. It
     * takes the parameter
     * {@link TailoringReferenceParameter#getReferencedElement()} and sets it as the
     * target of the corresponding link. When the
     * {@link TailoringReferenceParameter#getReferencedElement()} is null the link
     * will be removed from the element and linked later in
     * {@link #processInternalLinks(List, List)}.
     */
    private List<InternalResolveInfo> applyLinkTailoringReferences(Element copyItem,
            List<TailoringReferenceParameter> referencesToApply, CatalogItem catalogItem,
            Domain domain) {
        List<CustomLink> orderByExecution = copyItem.getLinks()
                                                    .stream()
                                                    .sorted(CustomLinkComparators.BY_LINK_EXECUTION)
                                                    .collect(Collectors.toList());

        if (orderByExecution.size() > referencesToApply.size()) {
            throw new IllegalArgumentException(
                    "Number of defined links cannot be smaller than number of references to apply.");
        }
        List<InternalResolveInfo> internalLinks = new ArrayList<>();
        List<TailoringReference> trefList = linkTailorReferences(catalogItem);
        for (int i = 0; i < orderByExecution.size(); i++) {
            CustomLink customLink = orderByExecution.get(i);
            TailoringReferenceParameter parameter = referencesToApply.get(i);
            if (parameter.getReferencedElement() == null) {
                TailoringReference tailoringReference = trefList.get(i);
                internalLinks.add(new InternalResolveInfo(
                        copyLink(copyItem, customLink.getTarget(), domain, customLink), copyItem,
                        tailoringReference.getCatalogItem()));
                copyItem.getLinks()
                        .remove(customLink);
            } else {
                customLink.setTarget(parameter.getReferencedElement());
            }
        }
        // TODO: VEO-612 handle parts
        return internalLinks;
    }

    /**
     * Apply the parameters from the incarnation description to the newly created
     * object. This method handles the {@link TailoringReferenceType#LINK_EXTERNAL}
     * type. It copies the link defined in
     * {@link ExternalTailoringReference#getExternalLink()}, sets the
     * linktargetEntity as the target of this link and adds it to the
     * {@link TailoringReferenceParameter#getReferencedElement()} and saves it. As
     * the {@link TailoringReferenceType#LINK_EXTERNAL} and
     * {@link TailoringReferenceType#LINK} are symmetrical, for each
     * {@link TailoringReferenceType#LINK} there needs to be an opposite
     * {@link TailoringReferenceType#LINK_EXTERNAL} pointing to each other. We
     * should not create such a link when the
     * {@link TailoringReferenceParameter#getReferencedElement()} is null, as we
     * demand the set of objects to create in one batch is complete and therefore
     * the link gets created by the {@link TailoringReferenceType#LINK} of the other
     * element.
     */
    private void applyExternalTailoringReferences(Element linkTargetEntity, Domain domain,
            List<ExternalTailoringReference> externalTailoringRefs,
            List<TailoringReferenceParameter> referencesToApply) {
        Iterator<TailoringReferenceParameter> parameter = referencesToApply.iterator();
        for (ExternalTailoringReference catalogReference : externalTailoringRefs) {
            TailoringReferenceParameter tailoringReferenceParameter = parameter.next();
            Element element = tailoringReferenceParameter.getReferencedElement();
            if (element != null) {
                copyLink(element, linkTargetEntity, domain, catalogReference.getExternalLink());
                saveElement(element);
            }
        }
    }

    /**
     * Creates a new link between source and target, as a value copy of the
     * linkToCopy. Adds the domain to this link.
     */
    private CustomLink copyLink(Element source, Element target, Domain domain,
            CustomLink linkToCopy) {
        CustomLink link = factory.createCustomLink(target, source, linkToCopy.getType());
        link.setAttributes(linkToCopy.getAttributes() == null ? null
                : new HashMap<>(linkToCopy.getAttributes()));
        link.addToDomains(domain);
        link.setType(linkToCopy.getType());
        source.addToLinks(link);
        return link;
    }

    /**
     * Links all {@code resolveInfo} objects with elements created in this batch.
     * Throws an error when the target is not part of the set of created elements.
     */
    private void processInternalLinks(List<InternalResolveInfo> internalLinks,
            List<Element> createdCatalogables) {
        for (InternalResolveInfo ri : internalLinks) {
            Element internalTarget = createdCatalogables.stream()
                                                        .filter(c -> c.getAppliedCatalogItems()
                                                                      .contains(ri.sourceItem))
                                                        .findFirst()
                                                        .orElseThrow(() -> new NotFoundException(
                                                                "CatalogItem %s:%s not included in request but required by %s:%s.",
                                                                ri.sourceItem.getNamespace(),
                                                                ri.sourceItem.getDisplayName(),
                                                                ri.source.getDesignator(),
                                                                ri.source.getName()));
            ri.link.setTarget(internalTarget);
            ri.source.addToLinks(ri.link);
        }
    }

    /**
     * Validates the incarnation parameters for the catalogItem against the catalog
     * description. (aka Tailoringreferences).
     */
    private void validateItem(CatalogItem catalogItem,
            List<TailoringReferenceParameter> referencesToApply) {
        if (linkTailorReferences(catalogItem).size() != linkTailorReferencesParameters(referencesToApply).size()) {
            throw new IllegalArgumentException("Tailoring references (LINK) don't match.");
        }
        if (externalTailorReferences(catalogItem).size() != externalTailorReferencesParameters(referencesToApply).size()) {
            throw new IllegalArgumentException("Tailoring references (EXTERNAL_LINK) don't match.");
        }
    }

    private Element saveElement(Element entity) {
        @SuppressWarnings("unchecked")
        ElementRepository<Element> repository = repositoryProvider.getElementRepositoryFor((Class<Element>) entity.getModelInterface());
        return repository.save(entity);
    }

    /**
     * Return the list of TailoringReference filtered by IS_LINK_PREDICATE and
     * ordered BY_EXECUTION for the given catalogItem.
     */
    private static List<TailoringReference> linkTailorReferences(CatalogItem catalogItem) {
        return catalogItem.getTailoringReferences()
                          .stream()
                          .filter(TailoringReferenceTyped.IS_LINK_PREDICATE)
                          .sorted(TailoringReferenceComparators.BY_EXECUTION)
                          .collect(Collectors.toList());
    }

    /**
     * Return the list of ExternalTailoringReference filtered by IS_LINK_PREDICATE
     * and ordered BY_EXECUTION for the given catalogItem.
     */
    private static List<ExternalTailoringReference> externalTailorReferences(
            CatalogItem catalogItem) {
        return catalogItem.getTailoringReferences()
                          .stream()
                          .filter(TailoringReferenceTyped.IS_EXTERNALLINK_PREDICATE)
                          .sorted(TailoringReferenceComparators.BY_EXECUTION)
                          .map(ExternalTailoringReference.class::cast)
                          .collect(Collectors.toList());
    }

    /**
     * Return a list of TailoringReferenceParameter filtered by IS_LINK_PREDICATE.
     */
    private static List<TailoringReferenceParameter> linkTailorReferencesParameters(
            List<TailoringReferenceParameter> referencesToApply) {
        return referencesToApply.stream()
                                .filter(TailoringReferenceTyped.IS_LINK_PREDICATE)
                                .collect(Collectors.toList());
    }

    /**
     * Return a list of TailoringReferenceParameter filtered by
     * IS_EXTERNALLINK_PREDICATE.
     */
    private static List<TailoringReferenceParameter> externalTailorReferencesParameters(
            List<TailoringReferenceParameter> referencesToApply) {
        return referencesToApply.stream()
                                .filter(TailoringReferenceTyped.IS_EXTERNALLINK_PREDICATE)
                                .collect(Collectors.toList());
    }

    @Valid
    @Value
    public static class InputData implements UseCase.InputData {
        Client authenticatedClient;
        Key<UUID> containerId;
        List<IncarnateCatalogItemDescription> referencesToApply;
    }

    @Valid
    @Value
    public static class OutputData implements UseCase.OutputData {
        @Valid
        List<Element> newElements;
    }

    /**
     * Contains the created elements and the links to resolve later of all these
     * elements.
     */
    @Data
    private static class IncarnationResult {
        private final List<Element> elements = new ArrayList<Element>();
        private final List<InternalResolveInfo> internalLinks = new ArrayList<InternalResolveInfo>();
    }

    /**
     * Contains the created element and the links to resolve later.
     */
    @Data
    @RequiredArgsConstructor
    private static class ElementResult {
        private final Element element;
        private final List<InternalResolveInfo> internalLinks;
    }

    @AllArgsConstructor
    private static class InternalResolveInfo {
        CustomLink link;
        Element source;
        CatalogItem sourceItem;
    }
}
