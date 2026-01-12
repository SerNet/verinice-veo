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

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.validation.constraints.NotNull;

import javax.annotation.Nullable;

import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainBase;
import org.veo.core.entity.Element;
import org.veo.core.entity.Identifiable;
import org.veo.core.entity.IncarnationConfiguration;
import org.veo.core.entity.IncarnationLookup;
import org.veo.core.entity.IncarnationRequestModeType;
import org.veo.core.entity.LinkTailoringReference;
import org.veo.core.entity.TailoringReference;
import org.veo.core.entity.TailoringReferenceType;
import org.veo.core.entity.TemplateItem;
import org.veo.core.entity.TemplateItemReference;
import org.veo.core.entity.Unit;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.entity.exception.RuntimeModelException;
import org.veo.core.entity.ref.TypedId;
import org.veo.core.entity.ref.TypedSymbolicId;
import org.veo.core.repository.GenericElementRepository;
import org.veo.core.repository.PagingConfiguration;
import org.veo.core.usecase.parameter.TailoringReferenceParameter;
import org.veo.core.usecase.parameter.TemplateItemIncarnationDescription;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class AbstractGetIncarnationDescriptionUseCase<
    T extends TemplateItem<T, TNamespace>, TNamespace extends Identifiable> {
  private final Class<T> itemType;
  private final GenericElementRepository genericElementRepository;

  protected List<TemplateItemIncarnationDescription<T, TNamespace>> getIncarnationDescriptions(
      @NotNull List<UUID> itemIdsInRequestedOrder,
      @NotNull Collection<T> items,
      @NotNull TNamespace namespace,
      @NotNull Domain domain,
      @NotNull Unit unit,
      @Nullable IncarnationRequestModeType requestType,
      @Nullable IncarnationLookup lookup,
      @Nullable Set<TailoringReferenceType> include,
      @Nullable Set<TailoringReferenceType> exclude,
      boolean mergeBidirectionalReferences) {
    var requestedItems = loadTemplateItems(itemIdsInRequestedOrder, items, namespace);
    var config = createConfig(requestType, lookup, exclude, include, domain);
    var tailoringReferenceFilter = config.createTailoringReferenceFilter();
    var itemsToElements =
        collectAllItems(
            requestedItems,
            tailoringReferenceFilter,
            config.mode(),
            config.useExistingIncarnations(),
            unit,
            domain);

    return getList(
        itemIdsInRequestedOrder,
        itemsToElements,
        tailoringReferenceFilter,
        mergeBidirectionalReferences);
  }

  private List<TailoringReferenceParameter> toParameters(
      Collection<TailoringReference<T, TNamespace>> catalogItem,
      Map<T, Optional<Element>> itemsToElements) {
    return catalogItem.stream()
        .filter(TailoringReference::isParameterRef)
        .map(
            tr ->
                mapParameter(
                    tr,
                    Optional.ofNullable(itemsToElements.get(tr.getTarget()))
                        .flatMap(Function.identity())
                        .orElse(null)))
        .toList();
  }

  private TailoringReferenceParameter mapParameter(
      TailoringReference<T, ?> reference, Element element) {
    return switch (reference.getReferenceType()) {
      case PART,
          COMPOSITE,
          RISK,
          CONTROL_IMPLEMENTATION,
          REQUIREMENT_IMPLEMENTATION,
          SCOPE,
          MEMBER ->
          fromReference(reference, element);
      case LINK, LINK_EXTERNAL ->
          fromLinkReference((LinkTailoringReference<T, ?>) reference, element);
      default ->
          throw new IllegalArgumentException(
              "Unmapped tailoring reference type: " + reference.getReferenceType());
    };
  }

  private TailoringReferenceParameter fromReference(
      TailoringReference<T, ?> linkReference, Element element) {
    TailoringReferenceParameter tailoringReferenceParameter =
        new TailoringReferenceParameter(linkReference.getReferenceType(), null);
    tailoringReferenceParameter.setReferencedElement(element);
    tailoringReferenceParameter.setId(linkReference.getIdAsString());
    return tailoringReferenceParameter;
  }

  /** Create a parameter object for this {@link LinkTailoringReference}. */
  private TailoringReferenceParameter fromLinkReference(
      LinkTailoringReference<T, ?> linkReference, Element element) {
    if (linkReference.getLinkType() == null) {
      throw new RuntimeModelException(
          "LinkType should not be null affected TailoringReferences: " + linkReference.getId());
    }
    TailoringReferenceParameter tailoringReferenceParameter =
        new TailoringReferenceParameter(
            linkReference.getReferenceType(), linkReference.getLinkType());
    tailoringReferenceParameter.setId(linkReference.getIdAsString());
    tailoringReferenceParameter.setReferencedElement(element);
    return tailoringReferenceParameter;
  }

  private IncarnationConfiguration createConfig(
      IncarnationRequestModeType requestType,
      IncarnationLookup useExistingIncarnations,
      Set<TailoringReferenceType> exclude,
      Set<TailoringReferenceType> include,
      DomainBase domain) {
    // Include and exclude lists are so closely related that it makes no sense to override them
    // individually.
    var overrideTailoringRefs = include != null || exclude != null;
    var defaultConfig = domain.getIncarnationConfiguration();
    return new IncarnationConfiguration(
        Optional.ofNullable(requestType).orElse(defaultConfig.mode()),
        Optional.ofNullable(useExistingIncarnations)
            .orElse(defaultConfig.useExistingIncarnations()),
        overrideTailoringRefs ? include : defaultConfig.include(),
        overrideTailoringRefs ? exclude : defaultConfig.exclude());
  }

  /**
   * @return A map containing all given items as keys. For each item, the map value is either an
   *     existing incarnation of the item or {@link Optional#empty()} if no incarnation was found in
   *     the unit.
   */
  private Map<T, Optional<Element>> buildIncarnationMap(
      Collection<T> items, Unit unit, Domain domain, boolean addExistingIncarnations) {
    if (addExistingIncarnations) {
      var query = genericElementRepository.query(unit.getClient());
      query.whereOwnerIs(unit);
      query.whereAppliedItemIn(
          items.stream()
              .map(i -> i.findCatalogItem().orElse(null))
              .filter(Objects::nonNull)
              .toList(),
          domain);
      query.fetchAppliedCatalogItems();
      var elements = query.execute(PagingConfiguration.UNPAGED).resultPage();
      return items.stream()
          .collect(
              Collectors.toMap(
                  Function.identity(),
                  item -> elements.stream().filter(item::isAppliedTo).findAny()));
    } else {
      return items.stream()
          .collect(Collectors.toMap(Function.identity(), item -> Optional.empty()));
    }
  }

  private List<TemplateItemIncarnationDescription<T, TNamespace>> getList(
      List<UUID> templateItemIds,
      Map<T, Optional<Element>> itemsToElements,
      Predicate<TailoringReference<?, ?>> tailoringReferenceFilter,
      boolean mergeBidirectionalReferences) {
    var distinctTailoringRefKeys = new HashSet<String>();
    return itemsToElements.entrySet().stream()
        // Only create incarnation descriptions for items without an existing incarnation.
        .filter(itemToElement -> itemToElement.getValue().isEmpty())
        .map(Map.Entry::getKey)
        // Restore the original order from the input.
        .sorted(
            Comparator.comparingInt(
                item ->
                    templateItemIds.contains(item.getSymbolicId())
                        ? templateItemIds.indexOf(item.getSymbolicId())
                        : Integer.MAX_VALUE))
        .map(
            catalogItem ->
                new TemplateItemIncarnationDescription<>(
                    catalogItem,
                    toParameters(
                        mergeBidirectionalReferences
                            ? catalogItem.getTailoringReferences().stream()
                                .filter(tailoringReferenceFilter)
                                .filter(tr -> distinctTailoringRefKeys.add(toKey(tr)))
                                .toList()
                            : catalogItem.getTailoringReferences().stream()
                                .filter(tailoringReferenceFilter)
                                .toList(),
                        itemsToElements)))
        .toList();
  }

  private List<T> loadTemplateItems(
      List<UUID> itemsIds, Collection<T> items, TNamespace namespace) {
    var itemsById =
        items.stream().collect(Collectors.toMap(TemplateItem::getSymbolicId, Function.identity()));
    return itemsIds.stream()
        .map(
            id -> {
              var item = itemsById.get(id);
              if (item == null) {
                throw new NotFoundException(
                    TypedSymbolicId.from(id, itemType, TypedId.from(namespace)));
              }
              return item;
            })
        .flatMap(i -> i.getAllItemsToIncarnate().stream())
        .distinct()
        .toList();
  }

  /**
   * Takes a list of origin items and determines which items should be incarnated and which items
   * have an existing incarnation that should be used. Referenced items are also included (depending
   * on the mode).
   *
   * @param mode In {@link IncarnationRequestModeType#DEFAULT}, directly or indirectly referenced
   *     items are also included in the result. In {@link IncarnationRequestModeType#MANUAL}, only
   *     directly referenced items that have an existing incarnation are included.
   * @return A map containing all given items plus referenced items as keys. For each item, the map
   *     value is either an existing incarnation of the item or {@link Optional#empty()} if the item
   *     should be incarnated as a new element instead.
   */
  private Map<T, Optional<Element>> collectAllItems(
      List<T> requestedItems,
      Predicate<? super TailoringReference<?, ?>> tailoringReferenceFilter,
      IncarnationRequestModeType mode,
      IncarnationLookup lookup,
      Unit unit,
      Domain domain) {
    var current =
        buildIncarnationMap(requestedItems, unit, domain, lookup == IncarnationLookup.ALWAYS);
    var result = new HashMap<>(current);

    switch (mode) {
      case MANUAL -> {
        // Search for existing incarnations of directly referenced items (unless lookup behavior is
        // NEVER).
        // Referenced items without an existing incarnation are not incarnated automatically, but
        // must be handled manually by the user (hence the name "MANUAL").
        // If the user does not manually fix those unresolved references by adding an incarnation
        // description
        // for the target item or by using an existing element as a reference target, applying the
        // incarnation descriptions will fail.
        if (lookup == IncarnationLookup.NEVER) {
          break;
        }
        var referencedItems = getReferencedItems(current, result, tailoringReferenceFilter, false);
        buildIncarnationMap(referencedItems, unit, domain, true).entrySet().stream()
            .filter(itemToElement -> itemToElement.getValue().isPresent())
            .forEach(itemToElement -> result.put(itemToElement.getKey(), itemToElement.getValue()));
      }
      case DEFAULT -> {
        // Follow both direct and indirect tailoring references, walking the reference tree
        // breadth-first.
        // Search for existing incarnations on every level (unless lookup behavior is NEVER).
        // Referenced items without an existing incarnation will be incarnated automatically.
        while (!current.isEmpty()) {
          var nextLevelItems =
              getReferencedItems(
                  current, result, tailoringReferenceFilter, lookup == IncarnationLookup.ALWAYS);
          current =
              buildIncarnationMap(nextLevelItems, unit, domain, lookup != IncarnationLookup.NEVER);
          result.putAll(current);
        }
      }
    }
    return result;
  }

  private Set<T> getReferencedItems(
      Map<T, Optional<Element>> current,
      Map<T, Optional<Element>> encountered,
      Predicate<? super TailoringReference<?, ?>> referenceFilter,
      boolean followReferencesOfExistingIncarnations) {
    return current.entrySet().stream()
        // Only follow references of items without an existing incarnation.
        .filter(
            itemToElement ->
                itemToElement.getValue().isEmpty() || followReferencesOfExistingIncarnations)
        .map(Map.Entry::getKey)
        .map(TemplateItem::getTailoringReferences)
        .flatMap(Collection::stream)
        .filter(referenceFilter)
        .map(TemplateItemReference::getTarget)
        // Circular structures are handled by avoiding items that have already been
        // encountered.
        .filter(item -> !encountered.containsKey(item))
        .collect(Collectors.toSet());
  }

  private String toKey(TailoringReference<?, ?> tailoringReference) {
    String origin = tailoringReference.getOwner().getSymbolicIdAsString();
    String target = tailoringReference.getTargetRef().getSymbolicId().toString();
    return switch (tailoringReference.getReferenceType()) {
      case LINK ->
          toKey(
              TailoringReferenceType.LINK,
              origin,
              target,
              ((LinkTailoringReference<?, ?>) tailoringReference).getLinkType());
      case LINK_EXTERNAL ->
          toKey(
              TailoringReferenceType.LINK,
              target,
              origin,
              ((LinkTailoringReference<?, ?>) tailoringReference).getLinkType());
      case PART -> toKey(TailoringReferenceType.PART, origin, target, "");
      case COMPOSITE -> toKey(TailoringReferenceType.PART, target, origin, "");
      case SCOPE -> toKey(TailoringReferenceType.SCOPE, origin, target, "");
      case MEMBER -> toKey(TailoringReferenceType.SCOPE, target, origin, "");
      case RISK -> toKey(TailoringReferenceType.RISK, origin, target, "");
      case CONTROL_IMPLEMENTATION ->
          toKey(TailoringReferenceType.CONTROL_IMPLEMENTATION, origin, target, "");
      case REQUIREMENT_IMPLEMENTATION ->
          toKey(TailoringReferenceType.REQUIREMENT_IMPLEMENTATION, origin, target, "");
      default ->
          throw new IllegalArgumentException(
              "Unexpected tailoring reference type %s"
                  .formatted(tailoringReference.getReferenceType()));
    };
  }

  private String toKey(
      TailoringReferenceType type, String sourceId, String targetId, String linkType) {
    return new StringBuffer()
        .append(type.name())
        .append(sourceId)
        .append(targetId)
        .append(linkType)
        .toString();
  }
}
