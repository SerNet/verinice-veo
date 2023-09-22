/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Urs Zeidler
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.veo.core.entity.CatalogItem;
import org.veo.core.entity.Client;
import org.veo.core.entity.CompositeElement;
import org.veo.core.entity.CustomLink;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;
import org.veo.core.entity.Key;
import org.veo.core.entity.LinkTailoringReference;
import org.veo.core.entity.TailoringReference;
import org.veo.core.entity.TailoringReferenceType;
import org.veo.core.entity.TailoringReferenceTyped;
import org.veo.core.entity.TemplateItem;
import org.veo.core.entity.Unit;
import org.veo.core.entity.exception.UnprocessableDataException;
import org.veo.core.entity.transform.EntityFactory;
import org.veo.core.repository.DomainRepository;
import org.veo.core.repository.ElementRepository;
import org.veo.core.repository.UnitRepository;
import org.veo.core.usecase.DesignatorService;
import org.veo.core.usecase.UseCaseTools;
import org.veo.core.usecase.catalogitem.AbtractApplyIncarnationDescriptionUseCase.ElementResult;
import org.veo.core.usecase.catalogitem.AbtractApplyIncarnationDescriptionUseCase.IncarnationResult;
import org.veo.core.usecase.parameter.TailoringReferenceParameter;
import org.veo.core.usecase.parameter.TemplateItemIncarnationDescription;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Slf4j
public abstract class AbtractApplyIncarnationDescriptionUseCase<T extends TemplateItem<T>> {
  protected final DesignatorService designatorService;
  protected final EntityFactory factory;
  protected final DomainRepository domainRepository;
  protected final UnitRepository unitRepository;
  private final org.veo.core.repository.RepositoryProvider repositoryProvider;

  @Data
  @AllArgsConstructor
  protected static class InternalResolveInfo<T> implements TailoringReferenceTyped {
    Element source;
    T sourceItem;
    TailoringReferenceType referenceType;
    String linkType;
    Map<String, Object> attributes;
    Domain domain;
  }

  /** Contains the created element and the links to resolve later. */
  @Data
  @RequiredArgsConstructor
  protected static class ElementResult<T> {
    private final Element element;
    private final T item;
    private final List<InternalResolveInfo<T>> internalLinks;
  }

  /** Contains the created elements and the links to resolve later all these elements. */
  @Data
  protected static class IncarnationResult<T> {
    private final Map<T, Element> mapping = new HashMap<>();
    private final List<Element> elements = new ArrayList<>();
    private final List<InternalResolveInfo<T>> internalLinks = new ArrayList<>();
  }

  /** Consumer to collect the IncarnationResult from the ElementResult. */
  protected BiConsumer<IncarnationResult<T>, ElementResult<T>> collectElementData() {
    return (elementData, iElement) -> {
      elementData.getElements().add(iElement.getElement());
      elementData.getInternalLinks().addAll(iElement.getInternalLinks());
      elementData.getMapping().put(iElement.getItem(), iElement.getElement());
    };
  }

  /** Update the IncarnationResult with the sub result. */
  protected BinaryOperator<IncarnationResult<T>> combineElementData() {
    return (source, data) -> {
      source.getElements().addAll(data.getElements());
      source.getInternalLinks().addAll(data.getInternalLinks());
      return source;
    };
  }

  /**
   * Creates a new link between source and target, as a value copy of the linkToCopy. Adds the
   * domain to this link.
   */
  protected CustomLink createLink(
      Element source, Element target, Domain domain, String type, Map<String, Object> attributes) {
    log.info("create link: {}", type);
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
  protected void processInternalLinks(
      List<InternalResolveInfo<T>> internalLinks, List<Element> createdCatalogables) {
    log.info("process internal links: {}", internalLinks.size());
    internalLinks.stream()
        .filter(TailoringReferenceTyped.IS_ALL_LINK_PREDICATE)
        .forEach(
            ri -> {
              Element internalTarget =
                  createdCatalogables
                      .stream() // the element has the corresponding catalogitem applied
                      .filter(c -> c.getAppliedCatalogItems().contains(ri.getSourceItem()))
                      .findFirst()
                      .orElseThrow(() -> throwInvalidTarget(ri));
              CustomLink link =
                  createLink(ri.source, internalTarget, ri.domain, ri.linkType, ri.attributes);
              ri.source.applyLink(link);
            });
  }

  protected void processParts(Map<T, Element> mapping, List<InternalResolveInfo<T>> internalLinks) {
    internalLinks.stream()
        .filter(TailoringReferenceTyped.IS_PART_PREDICATE)
        .forEach(
            il -> {
              Element element = mapping.get(il.sourceItem);
              if (element == null) {
                throw throwInvalidTarget(il);
              }
              addToPart(il.source, element);
            });
    internalLinks.stream()
        .filter(TailoringReferenceTyped.IS_COMPOSITE_PREDICATE)
        .forEach(
            il -> {
              Element element = mapping.get(il.sourceItem);
              if (element == null) {
                throw throwInvalidTarget(il);
              }
              addToPart(element, il.source);
            });
  }

  /**
   * Incarnate a catalogItem, uses the {@link CatalogItem#incarnate()} to create the element. Set
   * the customLinkTargets of this element to the given referencesToApply. Assign the designator,
   * save the element and create the links in the opposite objects which are defined by the {@link
   * TailoringReference}.
   *
   * @param tailrReferences
   */
  protected ElementResult<T> createElementFromCatalogItem(
      Unit unit,
      Client authenticatedClient,
      T catalogItem,
      Set<TailoringReference<T>> tailorReferences,
      Domain domain,
      List<TailoringReferenceParameter> referencesToApply) {
    validateItem(tailorReferences, referencesToApply);
    Element entity = catalogItem.incarnate(unit);
    List<InternalResolveInfo<T>> internalLinks =
        applyLinkTailoringReferences(
            entity,
            domain,
            linkTailorReferencesParameters(
                referencesToApply, TailoringReferenceTyped.IS_LINK_PREDICATE),
            linkTailorReferences(tailorReferences, TailoringReferenceTyped.IS_LINK_PREDICATE));
    List<InternalResolveInfo<T>> internalpartsLinks = Collections.emptyList();

    entity.setOwner(unit);
    if (entity instanceof CompositeElement<?> composite) {
      internalpartsLinks =
          applyPartReferences(
              composite,
              referencesToApply,
              tailorReferences.stream()
                  .filter(TailoringReferenceTyped.IS_ALL_PART_PREDICATE)
                  .toList());
    }
    designatorService.assignDesignator(entity, authenticatedClient);
    entity = saveElement(entity);
    applyExternalTailoringReferences(
        entity,
        domain,
        linkTailorReferences(tailorReferences, TailoringReferenceTyped.IS_EXTERNALLINK_PREDICATE),
        linkTailorReferencesParameters(
            referencesToApply, TailoringReferenceTyped.IS_EXTERNALLINK_PREDICATE));

    List<InternalResolveInfo<T>> arrayList =
        new ArrayList<>(internalLinks.size() + internalpartsLinks.size());
    arrayList.addAll(internalLinks);
    arrayList.addAll(internalpartsLinks);
    return new ElementResult<T>(entity, catalogItem, arrayList);
  }

  protected void checkDomains(Client client, Map<Key<UUID>, T> catalogItemsbyId) {
    Set<Domain> usedDomains =
        catalogItemsbyId.values().stream()
            .map(ci -> ci.requireDomainMembership())
            .collect(Collectors.toSet());

    usedDomains.forEach(domain -> UseCaseTools.checkDomainBelongsToClient(client, domain));
  }

  private Element saveElement(Element entity) {
    @SuppressWarnings("unchecked")
    ElementRepository<Element> repository =
        repositoryProvider.getElementRepositoryFor((Class<Element>) entity.getModelInterface());
    return repository.save(entity);
  }

  protected UnprocessableDataException throwInvalidTarget(InternalResolveInfo<T> ri) {
    return new UnprocessableDataException(
        format(
            "%s %s:%s not included in request but required by %s:%s.",
            ri.sourceItem.getClass().getSimpleName(),
            ri.sourceItem.getName(),
            ri.sourceItem.getDisplayName(),
            ri.source.getDesignator(),
            ri.source.getName()));
  }

  protected abstract ElementResult<T> incarnateByDescription(
      Unit unit,
      Client authenticatedClient,
      Map<Key<UUID>, T> catalogItemsbyId,
      TemplateItemIncarnationDescription ra);

  /** Return a list of TailoringReferenceParameter filtered by {@code typePredicate}. */
  private List<TailoringReferenceParameter> linkTailorReferencesParameters(
      List<TailoringReferenceParameter> referencesToApply,
      Predicate<? super TailoringReferenceParameter> typePredicate) {
    return referencesToApply.stream().filter(typePredicate).toList();
  }

  private List<LinkTailoringReference<T>> linkTailorReferences(
      Set<TailoringReference<T>> tailorReferences,
      Predicate<? super TailoringReferenceTyped> typePredicate) {
    return tailorReferences.stream()
        .filter(typePredicate)
        .map(r -> ((LinkTailoringReference<T>) r))
        .toList();
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
      List<LinkTailoringReference<T>> externalTailoringRefs,
      List<TailoringReferenceParameter> referencesToApply) {
    referencesToApply.stream()
        .forEach(
            tailoringReferenceParameter -> {
              LinkTailoringReference<T> catalogReference =
                  externalTailoringRefs.stream()
                      .filter(
                          t -> t.getId().uuidValue().equals(tailoringReferenceParameter.getId()))
                      .findAny()
                      .orElseThrow(() -> throwUnmappedPartExceprion(tailoringReferenceParameter));
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
   * Apply the parameters from the incarnation description to the newly created object. This method
   * handles the {@link TailoringReferenceType#LINK} type. It takes the parameter {@link
   * TailoringReferenceParameter#getReferencedElement()} and sets it as the target of the
   * corresponding link. When the {@link TailoringReferenceParameter#getReferencedElement()} is null
   * the link will be removed from the element and linked later in {@link
   * #processInternalLinks(List, List)}.
   *
   * @param catalogItem
   */
  private List<InternalResolveInfo<T>> applyLinkTailoringReferences(
      Element copyItem,
      Domain domain,
      List<TailoringReferenceParameter> referencesToApply,
      List<LinkTailoringReference<T>> linkTailoringReferences) {
    List<InternalResolveInfo<T>> internalLinks = new ArrayList<>(linkTailoringReferences.size());
    referencesToApply.stream()
        .forEach(
            referenceParameter -> {
              LinkTailoringReference<T> linkTailoringReference =
                  linkTailoringReferences.stream()
                      .filter(t -> t.getId().uuidValue().equals(referenceParameter.getId()))
                      .findAny()
                      .orElseThrow(() -> throwUnmappedPartExceprion(referenceParameter));
              if (referenceParameter.getReferencedElement() == null) {
                internalLinks.add(
                    new InternalResolveInfo<T>(
                        copyItem,
                        linkTailoringReference.getTarget(),
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

  private List<InternalResolveInfo<T>> applyPartReferences(
      Element entity,
      List<TailoringReferenceParameter> referencesToApply,
      List<TailoringReference<T>> tailoringReferences) {
    log.debug("applyPartReferences {},{},{}", entity, tailoringReferences, referencesToApply);
    List<InternalResolveInfo<T>> internalLinks = new ArrayList<>(tailoringReferences.size());
    referencesToApply.stream()
        .filter(TailoringReferenceTyped.IS_PART_PREDICATE)
        .forEach(
            referenceParameter -> {
              TailoringReference<T> reference =
                  tailoringReferences.stream()
                      .filter(tr -> tr.getIdAsString().equals(referenceParameter.getId()))
                      .findAny()
                      .orElseThrow(() -> throwUnmappedPartExceprion(referenceParameter));
              if (referenceParameter.getReferencedElement() == null) {
                internalLinks.add(
                    new InternalResolveInfo<T>(
                        entity,
                        reference.getTarget(),
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
              TailoringReference<T> reference =
                  tailoringReferences.stream()
                      .filter(tr -> tr.getIdAsString().equals(referenceParameter.getId()))
                      .findAny()
                      .orElseThrow(() -> throwUnmappedPartExceprion(referenceParameter));
              if (referenceParameter.getReferencedElement() == null) {
                internalLinks.add(
                    new InternalResolveInfo<T>(
                        entity,
                        reference.getTarget(),
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

  private IllegalArgumentException throwUnmappedPartExceprion(
      TailoringReferenceParameter referenceParameter) {
    return new IllegalArgumentException(
        String.format(
            "Unmapped %s tailoring reference. id:%s",
            referenceParameter.getReferenceType().name(), referenceParameter.getId()));
  }

  /**
   * Validates the incarnation parameters for the catalogItem against the catalog description. (aka
   * Tailoringreferences).
   */
  private void validateItem(
      Set<TailoringReference<T>> tailorReferences,
      List<TailoringReferenceParameter> referencesToApply) {
    if (linkTailorReferences(tailorReferences, TailoringReferenceTyped.IS_LINK_PREDICATE).size()
        != linkTailorReferencesParameters(
                referencesToApply, TailoringReferenceTyped.IS_LINK_PREDICATE)
            .size()) {
      throw new IllegalArgumentException("Tailoring references (LINK) don't match.");
    }
    if (linkTailorReferences(tailorReferences, TailoringReferenceTyped.IS_EXTERNALLINK_PREDICATE)
            .size()
        != linkTailorReferencesParameters(
                referencesToApply, TailoringReferenceTyped.IS_EXTERNALLINK_PREDICATE)
            .size()) {
      throw new IllegalArgumentException("Tailoring references (EXTERNAL_LINK) don't match.");
    }
  }

  @SuppressWarnings("unchecked")
  private void addToPart(Element element, Element part) {
    ((CompositeElement<CompositeElement<?>>) element)
        .addPart((CompositeElement<CompositeElement<?>>) part);
  }
}
