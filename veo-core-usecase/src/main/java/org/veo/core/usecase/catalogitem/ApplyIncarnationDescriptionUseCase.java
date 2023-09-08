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

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import jakarta.validation.Valid;

import org.veo.core.entity.CatalogItem;
import org.veo.core.entity.Client;
import org.veo.core.entity.CompositeElement;
import org.veo.core.entity.CustomLink;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;
import org.veo.core.entity.Identifiable;
import org.veo.core.entity.Key;
import org.veo.core.entity.LinkTailoringReference;
import org.veo.core.entity.TailoringReference;
import org.veo.core.entity.TailoringReferenceType;
import org.veo.core.entity.TailoringReferenceTyped;
import org.veo.core.entity.Unit;
import org.veo.core.entity.exception.ReferenceTargetNotFoundException;
import org.veo.core.entity.exception.UnprocessableDataException;
import org.veo.core.entity.transform.EntityFactory;
import org.veo.core.repository.CatalogItemRepository;
import org.veo.core.repository.DomainRepository;
import org.veo.core.repository.ElementRepository;
import org.veo.core.repository.UnitRepository;
import org.veo.core.usecase.DesignatorService;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.UseCaseTools;
import org.veo.core.usecase.parameter.TemplateItemIncarnationDescription;
import org.veo.core.usecase.parameter.TailoringReferenceParameter;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

/** Uses a list of {@link TemplateItemIncarnationDescription} to create items from a catalog in a unit. */
@Slf4j
@AllArgsConstructor
public class ApplyIncarnationDescriptionUseCase
    implements TransactionalUseCase<
        ApplyIncarnationDescriptionUseCase.InputData,
        ApplyIncarnationDescriptionUseCase.OutputData> {
  private final UnitRepository unitRepository;
  private final CatalogItemRepository catalogItemRepository;
  private final DomainRepository domainRepository;
  private final org.veo.core.repository.RepositoryProvider repositoryProvider;
  private final DesignatorService designatorService;
  private final EntityFactory factory;

  @Override
  public OutputData execute(InputData input) {
    // TODO: verinice-veo#2357 refactor this usecase
    log.info("ApplyIncarnationDescriptionUseCase: {}", input);
    Unit unit = unitRepository.getByIdFetchClient(input.getContainerId());
    Client authenticatedClient = input.authenticatedClient;
    unit.checkSameClient(authenticatedClient);
    List<TemplateItemIncarnationDescription> referencesToApply = input.getReferencesToApply();
    Set<Key<UUID>> catalogItemIds =
        referencesToApply.stream()
            .map(TemplateItemIncarnationDescription::getItem)
            .map(Identifiable::getId)
            .collect(Collectors.toSet());
    Map<Key<UUID>, CatalogItem> catalogItemsbyId =
        catalogItemRepository.findAllByIdsFetchDomainAndTailoringReferences(catalogItemIds).stream()
            .collect(Collectors.toMap(CatalogItem::getId, Function.identity()));

    Supplier<IncarnationResult> supplier = IncarnationResult::new;
    BiConsumer<IncarnationResult, ElementResult> consumer =
        (elementData, iElement) -> {
          elementData.elements.add(iElement.element);
          elementData.internalLinks.addAll(iElement.getInternalLinks());
          elementData.mapping.put(iElement.getItem(), iElement.element);
        };
    BinaryOperator<IncarnationResult> operator =
        (source, data) -> {
          source.elements.addAll(data.elements);
          source.internalLinks.addAll(data.internalLinks);
          return source;
        };
    Function<IncarnationResult, List<Element>> function =
        elementData -> {
          processInternalLinks(elementData.internalLinks, elementData.elements);
          processParts(elementData.getMapping(), elementData.internalLinks);
          return elementData.elements;
        };

    Set<Key<UUID>> usedDomains1 =
        catalogItemsbyId.values().stream()
            .map(ci -> ci.getOwner().getId())
            .collect(Collectors.toSet());

    Map<Key<UUID>, Domain> usedDomains =
        usedDomains1.stream()
            .collect(Collectors.toMap(Function.identity(), domainRepository::getById));

    usedDomains
        .values()
        .forEach(
            domain ->
                UseCaseTools.checkDomainBelongsToClient(input.getAuthenticatedClient(), domain));

    List<Element> createdElements =
        input.getReferencesToApply().stream()
            .map(
                ra -> {
                  Key<UUID> catalogItemId = ra.getItem().getId();
                  CatalogItem catalogItem = catalogItemsbyId.get(catalogItemId);
                  if (catalogItem == null) {
                    throw new ReferenceTargetNotFoundException(catalogItemId, CatalogItem.class);
                  }
                  Domain domain = usedDomains.get(catalogItem.getOwner().getId());
                  return createElementFromCatalogItem(
                      unit, authenticatedClient, catalogItem, domain, ra.getReferences());
                })
            .collect(Collector.of(supplier, consumer, operator, function));
    log.info("ApplyIncarnationDescriptionUseCase elements created: {}", createdElements);
    return new ApplyIncarnationDescriptionUseCase.OutputData(createdElements);
  }

  private void processParts(
      Map<CatalogItem, Element> mapping, List<InternalResolveInfo> internalLinks) {
    internalLinks.stream()
        .filter(TailoringReferenceTyped.IS_PART_PREDICATE)
        .forEach(
            il -> {
              Element element = mapping.get(il.sourceItem);

              if (element == null) {
                throw new UnprocessableDataException(
                    format(
                        "CatalogItem %s:%s not included in request but required by %s:%s.",
                        il.sourceItem.getNamespace(),
                        il.sourceItem.getDisplayName(),
                        il.source.getDesignator(),
                        il.source.getName()));
              }

              addToPart(il.source, element);
            });
    internalLinks.stream()
        .filter(TailoringReferenceTyped.IS_COMPOSITE_PREDICATE)
        .forEach(
            il -> {
              Element element = mapping.get(il.sourceItem);

              if (element == null) {
                throw new UnprocessableDataException(
                    format(
                        "CatalogItem %s:%s not included in request but required by %s:%s.",
                        il.sourceItem.getNamespace(),
                        il.sourceItem.getDisplayName(),
                        il.source.getDesignator(),
                        il.source.getName()));
              }
              addToPart(element, il.source);
            });
  }

  @SuppressWarnings("unchecked")
  private void addToPart(Element element, Element part) {
    ((CompositeElement<CompositeElement<?>>) element)
        .addPart((CompositeElement<CompositeElement<?>>) part);
  }

  /**
   * Incarnate a catalogItem, uses the {@link CatalogItem#incarnate()} to create the element. Set
   * the customLinkTargets of this element to the given referencesToApply. Assign the designator,
   * save the element and create the links in the opposite objects which are defined by the {@link
   * TailoringReference}.
   */
  private ElementResult createElementFromCatalogItem(
      Unit unit,
      Client authenticatedClient,
      CatalogItem catalogItem,
      Domain domain,
      List<TailoringReferenceParameter> referencesToApply) {
    validateItem(catalogItem, referencesToApply);
    Element entity = catalogItem.incarnate();
    List<InternalResolveInfo> internalLinks =
        applyLinkTailoringReferences(
            entity,
            domain,
            linkTailorReferencesParameters(
                referencesToApply, TailoringReferenceTyped.IS_LINK_PREDICATE),
            linkTailorReferences(
                catalogItem.getTailoringReferences(), TailoringReferenceTyped.IS_LINK_PREDICATE));
    List<InternalResolveInfo> internalpartsLinks = Collections.emptyList();

    entity.setOwner(unit);
    if (entity instanceof CompositeElement<?> composite) {
      internalpartsLinks =
          applyPartReferences(
              composite,
              referencesToApply,
              catalogItem.getTailoringReferences().stream()
                  .filter(TailoringReferenceTyped.IS_ALL_PART_PREDICATE)
                  .toList());
    }
    designatorService.assignDesignator(entity, authenticatedClient);
    entity = saveElement(entity);
    applyExternalTailoringReferences(
        entity,
        domain,
        linkTailorReferences(
            catalogItem.getTailoringReferences(),
            TailoringReferenceTyped.IS_EXTERNALLINK_PREDICATE),
        linkTailorReferencesParameters(
            referencesToApply, TailoringReferenceTyped.IS_EXTERNALLINK_PREDICATE));

    List<InternalResolveInfo> arrayList =
        new ArrayList<>(internalLinks.size() + internalpartsLinks.size());
    arrayList.addAll(internalLinks);
    arrayList.addAll(internalpartsLinks);
    return new ElementResult(entity, catalogItem, arrayList);
  }

  private List<InternalResolveInfo> applyPartReferences(
      Element entity,
      List<TailoringReferenceParameter> referencesToApply,
      List<TailoringReference<CatalogItem>> tailoringReferences) {
    log.debug("applyPartReferences {},{},{}", entity, tailoringReferences, referencesToApply);
    List<InternalResolveInfo> internalLinks = new ArrayList<>(tailoringReferences.size());

    referencesToApply.stream()
        .filter(TailoringReferenceTyped.IS_PART_PREDICATE)
        .forEach(
            referenceParameter -> {
              TailoringReference<CatalogItem> reference =
                  tailoringReferences.stream()
                      .filter(tr -> tr.getIdAsString().equals(referenceParameter.getId()))
                      .findAny()
                      .orElseThrow(
                          () ->
                              new IllegalArgumentException(
                                  String.format(
                                      "Unmapped PART tailoring reference. id:%s",
                                      referenceParameter.getId())));

              if (referenceParameter.getReferencedElement() == null) {
                internalLinks.add(
                    new InternalResolveInfo(
                        entity,
                        reference.getCatalogItem(),
                        reference.getReferenceType(),
                        reference.getReferenceType().name(),
                        null,
                        null));
              } else {
                addToPart(entity, referenceParameter.getReferencedElement());
              }
            });

    referencesToApply.stream()
        .filter(TailoringReferenceTyped.IS_COMPOSITE_PREDICATE)
        .forEach(
            referenceParameter -> {
              TailoringReference<CatalogItem> reference =
                  tailoringReferences.stream()
                      .filter(tr -> tr.getIdAsString().equals(referenceParameter.getId()))
                      .findAny()
                      .orElseThrow(
                          () ->
                              new IllegalArgumentException(
                                  String.format(
                                      "Unmapped %s tailoring reference. id:%s",
                                      referenceParameter.getReferenceKey(),
                                      referenceParameter.getId())));

              if (referenceParameter.getReferencedElement() == null) {
                internalLinks.add(
                    new InternalResolveInfo(
                        entity,
                        reference.getCatalogItem(),
                        reference.getReferenceType(),
                        null,
                        null,
                        null));
              } else {
                addToPart(referenceParameter.getReferencedElement(), entity);
              }
            });

    return internalLinks;
  }

  /**
   * Apply the parameters from the incarnation description to the newly created object. This method
   * handles the {@link TailoringReferenceType#LINK} type. It takes the parameter {@link
   * TailoringReferenceParameter#getReferencedElement()} and sets it as the target of the
   * corresponding link. When the {@link TailoringReferenceParameter#getReferencedElement()} is null
   * the link will be removed from the element and linked later in {@link
   * #processInternalLinks(List, List)}.
   *
   * @param catalogItem
   */
  private List<InternalResolveInfo> applyLinkTailoringReferences(
      Element copyItem,
      Domain domain,
      List<TailoringReferenceParameter> referencesToApply,
      List<LinkTailoringReference<CatalogItem>> linkTailoringReferences) {
    List<InternalResolveInfo> internalLinks = new ArrayList<>(linkTailoringReferences.size());

    referencesToApply.stream()
        .forEach(
            referenceParameter -> {
              LinkTailoringReference<CatalogItem> linkTailoringReference =
                  linkTailoringReferences.stream()
                      .filter(t -> t.getId().uuidValue().equals(referenceParameter.getId()))
                      .findAny()
                      .orElseThrow(
                          () -> new IllegalArgumentException("Unmapped tailoring reference."));
              if (referenceParameter.getReferencedElement() == null) {
                internalLinks.add(
                    new InternalResolveInfo(
                        copyItem,
                        linkTailoringReference.getCatalogItem(),
                        linkTailoringReference.getReferenceType(),
                        linkTailoringReference.getLinkType(),
                        linkTailoringReference.getAttributes(),
                        domain));
              } else {
                createLink(
                    copyItem,
                    referenceParameter.getReferencedElement(),
                    domain,
                    linkTailoringReference.getLinkType(),
                    linkTailoringReference.getAttributes());
              }
            });
    return internalLinks;
  }

  /**
   * Apply the parameters from the incarnation description to the newly created object. This method
   * handles the {@link TailoringReferenceType#LINK_EXTERNAL} type. It copies the link defined in
   * {@link ExternalTailoringReference#getExternalLink()}, sets the linktargetEntity as the target
   * of this link and adds it to the {@link TailoringReferenceParameter#getReferencedElement()} and
   * saves it. As the {@link TailoringReferenceType#LINK_EXTERNAL} and {@link
   * TailoringReferenceType#LINK} are symmetrical, for each {@link TailoringReferenceType#LINK}
   * there needs to be an opposite {@link TailoringReferenceType#LINK_EXTERNAL} pointing to each
   * other. We should not create such a link when the {@link
   * TailoringReferenceParameter#getReferencedElement()} is null, as we demand the set of objects to
   * create in one batch is complete and therefore the link gets created by the {@link
   * TailoringReferenceType#LINK} of the other element.
   */
  private void applyExternalTailoringReferences(
      Element linkTargetEntity,
      Domain domain,
      List<LinkTailoringReference<CatalogItem>> externalTailoringRefs,
      List<TailoringReferenceParameter> referencesToApply) {

    referencesToApply.stream()
        .forEach(
            tailoringReferenceParameter -> {
              LinkTailoringReference<CatalogItem> catalogReference =
                  externalTailoringRefs.stream()
                      .filter(
                          t -> t.getId().uuidValue().equals(tailoringReferenceParameter.getId()))
                      .findAny()
                      .orElseThrow(
                          () ->
                              new IllegalArgumentException(
                                  String.format(
                                      "Unmapped %s tailoring reference. id:%s",
                                      tailoringReferenceParameter.getReferenceKey(),
                                      tailoringReferenceParameter.getId())));
              Element element = tailoringReferenceParameter.getReferencedElement();
              if (element != null) {
                if (element.getDomains() == null || !element.getDomains().contains(domain)) {
                  throw new IllegalArgumentException(
                      "The element to link is not part of the domain: "
                          + element.getDesignator()
                          + "  "
                          + domain.getName());
                }
                createLink(
                    element,
                    linkTargetEntity,
                    domain,
                    catalogReference.getLinkType(),
                    catalogReference.getAttributes());
                saveElement(element);
              }
            });
  }

  /**
   * Creates a new link between source and target, as a value copy of the linkToCopy. Adds the
   * domain to this link.
   */
  private CustomLink createLink(
      Element source, Element target, Domain domain, String type, Map<String, Object> attributes) {
    CustomLink link = factory.createCustomLink(target, source, type, domain);
    link.setAttributes(attributes == null ? new HashMap<>() : new HashMap<>(attributes));
    link.setType(type);
    source.applyLink(link);
    return link;
  }

  /**
   * Links all {@code resolveInfo} objects with elements created in this batch. Throws an error when
   * the target is not part of the set of created elements.
   */
  private void processInternalLinks(
      List<InternalResolveInfo> internalLinks, List<Element> createdCatalogables) {

    internalLinks.stream()
        .filter(TailoringReferenceTyped.IS_ALL_LINK_PREDICATE)
        .forEach(
            ri -> {
              Element internalTarget =
                  createdCatalogables.stream()
                      .filter(c -> c.getAppliedCatalogItems().contains(ri.sourceItem))
                      .findFirst()
                      .orElseThrow(
                          () ->
                              new UnprocessableDataException(
                                  format(
                                      "CatalogItem %s:%s not included in request but required by %s:%s.",
                                      ri.sourceItem.getNamespace(),
                                      ri.sourceItem.getDisplayName(),
                                      ri.source.getDesignator(),
                                      ri.source.getName())));
              CustomLink link =
                  createLink(ri.source, internalTarget, ri.domain, ri.linkType, ri.attributes);
              ri.source.applyLink(link);
            });
  }

  /**
   * Validates the incarnation parameters for the catalogItem against the catalog description. (aka
   * Tailoringreferences).
   */
  private void validateItem(
      CatalogItem catalogItem, List<TailoringReferenceParameter> referencesToApply) {
    if (linkTailorReferences(
                catalogItem.getTailoringReferences(), TailoringReferenceTyped.IS_LINK_PREDICATE)
            .size()
        != linkTailorReferencesParameters(
                referencesToApply, TailoringReferenceTyped.IS_LINK_PREDICATE)
            .size()) {
      throw new IllegalArgumentException("Tailoring references (LINK) don't match.");
    }
    if (linkTailorReferences(
                catalogItem.getTailoringReferences(),
                TailoringReferenceTyped.IS_EXTERNALLINK_PREDICATE)
            .size()
        != linkTailorReferencesParameters(
                referencesToApply, TailoringReferenceTyped.IS_EXTERNALLINK_PREDICATE)
            .size()) {
      throw new IllegalArgumentException("Tailoring references (EXTERNAL_LINK) don't match.");
    }
  }

  private Element saveElement(Element entity) {
    @SuppressWarnings("unchecked")
    ElementRepository<Element> repository =
        repositoryProvider.getElementRepositoryFor((Class<Element>) entity.getModelInterface());
    return repository.save(entity);
  }

  /**
   * Return the list of TailoringReference filtered by {@code typePredicate} and ordered
   * BY_EXECUTION for the given catalogItem.
   */
  private List<LinkTailoringReference<CatalogItem>> linkTailorReferences(
      Collection<TailoringReference<CatalogItem>> references,
      Predicate<? super TailoringReference<CatalogItem>> typePredicate) {
    return references.stream()
        .filter(typePredicate)
        .map(l -> (LinkTailoringReference<CatalogItem>) l)
        .toList();
  }

  /** Return a list of TailoringReferenceParameter filtered by {@code typePredicate}. */
  private List<TailoringReferenceParameter> linkTailorReferencesParameters(
      List<TailoringReferenceParameter> referencesToApply,
      Predicate<? super TailoringReferenceParameter> typePredicate) {
    return referencesToApply.stream().filter(typePredicate).toList();
  }

  @Override
  public boolean isReadOnly() {
    return false;
  }

  @Valid
  @Value
  public static class InputData implements UseCase.InputData {
    Client authenticatedClient;
    Key<UUID> containerId;
    List<TemplateItemIncarnationDescription> referencesToApply;
  }

  @Valid
  @Value
  public static class OutputData implements UseCase.OutputData {
    @Valid List<Element> newElements;
  }

  /** Contains the created elements and the links to resolve later all these elements. */
  @Data
  private static class IncarnationResult {
    private final Map<CatalogItem, Element> mapping = new HashMap<>();
    private final List<Element> elements = new ArrayList<>();
    private final List<InternalResolveInfo> internalLinks = new ArrayList<>();
  }

  /** Contains the created element and the links to resolve later. */
  @Data
  @RequiredArgsConstructor
  private static class ElementResult {
    private final Element element;
    private final CatalogItem item;
    private final List<InternalResolveInfo> internalLinks;
  }

  @Data
  @AllArgsConstructor
  private static class InternalResolveInfo implements TailoringReferenceTyped {
    Element source;
    CatalogItem sourceItem;
    TailoringReferenceType referenceType;
    String linkType;
    Map<String, Object> attributes;
    Domain domain;
  }
}
