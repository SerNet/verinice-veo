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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.veo.core.entity.CatalogItem;
import org.veo.core.entity.Catalogable;
import org.veo.core.entity.Client;
import org.veo.core.entity.CustomLink;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;
import org.veo.core.entity.ExternalTailoringReference;
import org.veo.core.entity.Key;
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
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

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

        List<Catalogable> createdCatalogables = input.getReferencesToApply()
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
                                                         return createElementFromItem(unit,
                                                                                      authenticatedClient,
                                                                                      catalogItem,
                                                                                      domain,
                                                                                      ra.getReferences());
                                                     })
                                                     .collect(Collectors.toList());
        log.info("ApplyIncarnationDescriptionUseCase elements created: {}", createdCatalogables);
        return new ApplyIncarnationDescriptionUseCase.OutputData(createdCatalogables);
    }

    private Element createElementFromItem(Unit unit, Client authenticatedClient,
            CatalogItem catalogItem, Domain domain,
            List<TailoringReferenceParameter> referencesToApply) {
        validateItem(catalogItem, referencesToApply);
        Catalogable copyItem = catalogItemservice.createInstance(catalogItem, domain);
        applyLinkTailoringReferences(copyItem, referencesToApply.stream()
                                                                .filter(TailoringReferenceTyped.IS_LINK_PREDICATE)
                                                                .collect(Collectors.toList()));
        @SuppressWarnings("unchecked")
        ElementRepository<Element> repository = repositoryProvider.getElementRepositoryFor((Class<Element>) copyItem.getModelInterface());
        Element entity = (Element) copyItem;
        entity.setOwner(unit);
        designatorService.assignDesignator(entity, authenticatedClient);
        entity = repository.save(entity);
        applyExternalTailoringReferences(entity, domain, externalTailorReferences(catalogItem),
                                         externalTailorReferencesParameters(referencesToApply));
        return entity;
    }

    private void applyLinkTailoringReferences(Catalogable copyItem,
            List<TailoringReferenceParameter> referencesToApply) {
        if (copyItem instanceof Element) {
            Element el = (Element) copyItem;
            List<CustomLink> orderByExecution = el.getLinks()
                                                  .stream()
                                                  .sorted(CustomLinkComparators.BY_LINK_EXECUTION)
                                                  .collect(Collectors.toList());

            if (orderByExecution.size() > referencesToApply.size()) {
                throw new IllegalArgumentException(
                        "Number of defined links cannot be smaller than number of references to apply.");
            }

            for (int i = 0; i < orderByExecution.size(); i++) {
                CustomLink customLink = orderByExecution.get(i);
                customLink.setTarget((Element) referencesToApply.get(i)
                                                                .getReferencedCatalogable());
            }
            // TODO: VEO-612 handle parts
        }
    }

    private void applyExternalTailoringReferences(Element linkTargetEntity, Domain domain,
            List<ExternalTailoringReference> externalTailoringRefs,
            List<TailoringReferenceParameter> referencesToApply) {
        Iterator<TailoringReferenceParameter> parameter = referencesToApply.iterator();
        for (ExternalTailoringReference catalogReference : externalTailoringRefs) {
            TailoringReferenceParameter tailoringReferenceParameter = parameter.next();
            Catalogable catalogable = tailoringReferenceParameter.getReferencedCatalogable();
            if (catalogable instanceof Element) {
                Element linkSourceEntity = (Element) catalogable;
                copyLink(linkSourceEntity, linkTargetEntity, domain,
                         catalogReference.getExternalLink());
                @SuppressWarnings("unchecked")
                ElementRepository<Element> repository = repositoryProvider.getElementRepositoryFor((Class<Element>) catalogable.getModelInterface());
                linkSourceEntity = repository.save(linkSourceEntity);
            }
        }
    }

    /**
     * Creates a new link between source and target, as a value copy of the
     * linkToCopy. Adds the domain to this link.
     */
    private void copyLink(Element source, Element target, Domain domain, CustomLink linkToCopy) {
        CustomLink link = factory.createCustomLink(target, source, linkToCopy.getType());
        link.setAttributes(linkToCopy.getAttributes() == null ? null
                : new HashMap<>(linkToCopy.getAttributes()));
        link.addToDomains(domain);
        link.setType(linkToCopy.getType());
        source.addToLinks(link);
    }

    private void validateItem(CatalogItem catalogItem,
            List<TailoringReferenceParameter> referencesToApply) {
        if (catalogItem.getTailoringReferences()
                       .stream()
                       .filter(TailoringReferenceTyped.IS_LINK_PREDICATE)
                       .count() != referencesToApply.stream()
                                                    .filter(r -> r.getReferenceType() == TailoringReferenceType.LINK)
                                                    .count()) {
            throw new IllegalArgumentException("Tailoring references (LINK) don't match.");
        }
        if (catalogItem.getTailoringReferences()
                       .stream()
                       .filter(r -> r.getReferenceType() == TailoringReferenceType.LINK_EXTERNAL)
                       .count() != referencesToApply.stream()
                                                    .filter(r -> r.getReferenceType() == TailoringReferenceType.LINK_EXTERNAL)
                                                    .count()) {
            throw new IllegalArgumentException("Tailoring references (EXTERNAL_LINK) don't match.");
        }
        if (referencesToApply.stream()
                             .anyMatch(t -> t.getReferencedCatalogable() == null)) {
            throw new IllegalArgumentException("Tailoring references target undefined.");// need to
                                                                                         // change
                                                                                         // with
                                                                                         // VEO-726
        }
    }

    private List<ExternalTailoringReference> externalTailorReferences(CatalogItem catalogItem) {
        return catalogItem.getTailoringReferences()
                          .stream()
                          .filter(TailoringReferenceTyped.IS_EXTERNALLINK_PREDICATE)
                          .sorted(TailoringReferenceComparators.BY_EXECUTION)
                          .map(ExternalTailoringReference.class::cast)
                          .collect(Collectors.toList());
    }

    private List<TailoringReferenceParameter> externalTailorReferencesParameters(
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
        List<Catalogable> newElements;
    }

}
