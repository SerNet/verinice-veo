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

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.veo.core.entity.CatalogItem;
import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainBase;
import org.veo.core.entity.Element;
import org.veo.core.entity.IncarnationLookup;
import org.veo.core.entity.IncarnationRequestModeType;
import org.veo.core.entity.TailoringReference;
import org.veo.core.entity.TailoringReferenceType;
import org.veo.core.entity.TemplateItem;
import org.veo.core.entity.TemplateItemReference;
import org.veo.core.entity.Unit;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.entity.ref.TypedId;
import org.veo.core.entity.ref.TypedSymbolicId;
import org.veo.core.repository.CatalogItemRepository;
import org.veo.core.repository.DomainRepository;
import org.veo.core.repository.GenericElementRepository;
import org.veo.core.repository.PagingConfiguration;
import org.veo.core.repository.UnitRepository;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.parameter.TemplateItemIncarnationDescription;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Slf4j
public class GetCatalogIncarnationDescriptionUseCase
    extends AbstractGetIncarnationDescriptionUseCase<CatalogItem, DomainBase>
    implements TransactionalUseCase<
        GetCatalogIncarnationDescriptionUseCase.InputData,
        GetCatalogIncarnationDescriptionUseCase.OutputData> {
  private final DomainRepository domainRepository;
  private final UnitRepository unitRepository;
  private final CatalogItemRepository catalogItemRepository;
  private final GenericElementRepository genericElementRepository;

  @Override
  public OutputData execute(InputData input) {
    log.info(
        "Creating incarnation descriptions for items {} from domain {} in unit: {}",
        input.catalogItemIds,
        input.domainId,
        input.unitId);
    Unit unit = unitRepository.getByIdFetchClient(input.unitId);
    unit.checkSameClient(input.authenticatedClient);
    validateInput(input);
    var domain = domainRepository.getActiveById(input.domainId, input.authenticatedClient.getId());
    var requestedItems = loadCatalogItems(input.catalogItemIds, domain);
    var config =
        createConfig(input.requestType, input.lookup, input.exclude, input.include, domain);
    var tailoringReferenceFilter = config.createTailoringReferenceFilter();
    var itemsToElements =
        collectAllItems(
            requestedItems,
            tailoringReferenceFilter,
            config.mode(),
            config.useExistingIncarnations(),
            unit,
            domain);
    var incarnationDescriptions =
        itemsToElements.entrySet().stream()
            // Only create incarnation descriptions for items without an existing incarnation.
            .filter(itemToElement -> itemToElement.getValue().isEmpty())
            .map(Map.Entry::getKey)
            // Restore the original order from the input.
            .sorted(
                Comparator.comparingInt(
                    item ->
                        input.catalogItemIds.contains(item.getSymbolicId())
                            ? input.catalogItemIds.indexOf(item.getSymbolicId())
                            : Integer.MAX_VALUE))
            .map(
                catalogItem ->
                    new TemplateItemIncarnationDescription<>(
                        catalogItem,
                        toParameters(
                            catalogItem.getTailoringReferences().stream()
                                .filter(tailoringReferenceFilter)
                                .toList(),
                            itemsToElements)))
            .toList();
    log.info("incarnation descriptions: {}", incarnationDescriptions);
    return new OutputData(incarnationDescriptions, unit);
  }

  private List<CatalogItem> loadCatalogItems(List<UUID> catalogItemIds, Domain domain) {
    var catalogItemsById =
        catalogItemRepository.findAllByIdsFetchTailoringReferences(catalogItemIds, domain).stream()
            .collect(Collectors.toMap(CatalogItem::getSymbolicId, Function.identity()));
    return catalogItemIds.stream()
        .map(
            id -> {
              var catalogItem = catalogItemsById.get(id);
              if (catalogItem == null) {
                throw new NotFoundException(
                    TypedSymbolicId.from(id, CatalogItem.class, TypedId.from(domain)));
              }
              return catalogItem;
            })
        .flatMap(ci -> ci.getAllItemsToIncarnate().stream())
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
  private Map<CatalogItem, Optional<Element>> collectAllItems(
      List<CatalogItem> requestedItems,
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

  private static Set<CatalogItem> getReferencedItems(
      Map<CatalogItem, Optional<Element>> current,
      Map<CatalogItem, Optional<Element>> encountered,
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

  private void validateInput(InputData input) {
    if (new HashSet<>(input.catalogItemIds).size() != input.catalogItemIds.size()) {
      throw new IllegalArgumentException("Provided catalog items are not unique.");
    }
  }

  /**
   * @return A map containing all given items as keys. For each item, the map value is either an
   *     existing incarnation of the item or {@link Optional#empty()} if no incarnation was found in
   *     the unit.
   */
  private Map<CatalogItem, Optional<Element>> buildIncarnationMap(
      Collection<CatalogItem> items, Unit unit, Domain domain, boolean addExistingIncarnations) {
    if (addExistingIncarnations) {
      var query = genericElementRepository.query(unit.getClient());
      query.whereOwnerIs(unit);
      query.whereAppliedItemIn(items, domain);
      query.fetchAppliedCatalogItems();
      var elements = query.execute(PagingConfiguration.UNPAGED).getResultPage();
      return items.stream()
          .collect(
              Collectors.toMap(
                  Function.identity(),
                  item ->
                      elements.stream()
                          .filter(
                              e -> e.findAppliedCatalogItem(domain).map(item::equals).orElse(false))
                          .findAny()));
    } else {
      return items.stream()
          .collect(Collectors.toMap(Function.identity(), item -> Optional.empty()));
    }
  }

  @Valid
  public record InputData(
      Client authenticatedClient,
      @NotNull UUID unitId,
      @NotNull UUID domainId,
      @NotNull List<UUID> catalogItemIds,
      IncarnationRequestModeType requestType,
      IncarnationLookup lookup,
      Set<TailoringReferenceType> include,
      Set<TailoringReferenceType> exclude)
      implements UseCase.InputData {}

  @Valid
  public record OutputData(
      @Valid List<TemplateItemIncarnationDescription<CatalogItem, DomainBase>> references,
      Unit container)
      implements UseCase.OutputData {}
}
