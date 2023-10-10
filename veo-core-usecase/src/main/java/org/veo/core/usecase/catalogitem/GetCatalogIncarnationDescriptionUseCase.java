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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.veo.core.entity.CatalogItem;
import org.veo.core.entity.Client;
import org.veo.core.entity.Element;
import org.veo.core.entity.Key;
import org.veo.core.entity.TailoringReference;
import org.veo.core.entity.TailoringReferenceType;
import org.veo.core.entity.TailoringReferenceTyped;
import org.veo.core.entity.TemplateItem;
import org.veo.core.entity.TemplateItemReference;
import org.veo.core.entity.Unit;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.repository.CatalogItemRepository;
import org.veo.core.repository.GenericElementRepository;
import org.veo.core.repository.PagingConfiguration;
import org.veo.core.repository.UnitRepository;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.parameter.TemplateItemIncarnationDescription;

import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Slf4j
public class GetCatalogIncarnationDescriptionUseCase
    extends AbstractGetIncarnationDescriptionUseCase<CatalogItem>
    implements TransactionalUseCase<
        GetCatalogIncarnationDescriptionUseCase.InputData,
        GetCatalogIncarnationDescriptionUseCase.OutputData> {
  private final UnitRepository unitRepository;
  private final CatalogItemRepository catalogItemRepository;
  private final GenericElementRepository genericElementRepository;

  @Override
  public OutputData execute(InputData input) {
    log.info("unid: {}, items: {}", input.unitId, input.catalogItemIds);
    Unit unit = unitRepository.getByIdFetchClient(input.getUnitId());
    unit.checkSameClient(input.authenticatedClient);
    validateInput(input);

    var tailoringReferenceFilter = createTailoringReferenceFilter(input.exclude, input.include);
    var requestedItems = loadCatalogItems(input.getCatalogItemIds(), input.authenticatedClient);
    var requestType =
        Optional.ofNullable(input.requestType).orElse(IncarnationRequestModeType.DEFAULT);
    var itemsToCreate =
        switch (requestType) {
          case DEFAULT -> collectAllItems(requestedItems, tailoringReferenceFilter);
          case MANUAL -> requestedItems;
        };

    var linkedCatalogItems =
        itemsToCreate.stream()
            .flatMap(
                catalogItem ->
                    catalogItem.getTailoringReferences().stream()
                        .filter(tailoringReferenceFilter)
                        .filter(TailoringReferenceTyped.IS_PARAMETER_REF)
                        .map(TailoringReference::getTarget))
            .toList();

    var existingIncarnationsByItem = findExistingIncarnations(unit, linkedCatalogItems);
    var incarnationDescriptions =
        itemsToCreate.stream()
            .map(
                catalogItem ->
                    new TemplateItemIncarnationDescription(
                        catalogItem,
                        toParameters(
                            catalogItem.getTailoringReferences().stream()
                                .filter(tailoringReferenceFilter)
                                .toList(),
                            existingIncarnationsByItem)))
            .toList();
    log.info("incarnation descriptions: {}", incarnationDescriptions);
    return new OutputData(incarnationDescriptions, unit);
  }

  private List<CatalogItem> loadCatalogItems(List<Key<UUID>> catalogItemIds, Client client) {
    var catalogItemsById =
        catalogItemRepository
            .findAllByIdsFetchDomainAndTailoringReferences(new HashSet<>(catalogItemIds), client)
            .stream()
            .collect(Collectors.toMap(CatalogItem::getId, Function.identity()));
    return catalogItemIds.stream()
        .map(
            id -> {
              var catalogItem = catalogItemsById.get(id);
              if (catalogItem == null) {
                throw new NotFoundException(id, CatalogItem.class);
              }
              return catalogItem;
            })
        .flatMap(ci -> ci.getAllItemsToIncarnate().stream())
        .distinct()
        .toList();
  }

  /**
   * Takes a list of origin items and finds all of their related items by recursively following
   * tailoring references.
   *
   * @return given origin items plus found related items
   */
  private List<CatalogItem> collectAllItems(
      List<CatalogItem> items,
      Predicate<? super TailoringReference<CatalogItem>> tailoringReferenceFilter) {
    List<CatalogItem> current = items;
    List<CatalogItem> next =
        Stream.concat(
                items.stream(),
                items.stream()
                    .map(TemplateItem::getTailoringReferences)
                    .flatMap(Collection::stream)
                    .filter(tailoringReferenceFilter)
                    .map(TemplateItemReference::getTarget))
            .distinct()
            .toList();
    while (current.size() < next.size()) {
      current = collectAllItems(next, tailoringReferenceFilter);
    }
    return current;
  }

  private void validateInput(InputData input) {
    if (new HashSet<>(input.catalogItemIds).size() != input.catalogItemIds.size()) {
      throw new IllegalArgumentException("Provided catalog items are not unique.");
    }
  }

  /** Searches for {@link Element}s in the unit which have the given catalogItems applied. */
  private Map<CatalogItem, Element> findExistingIncarnations(
      Unit unit, Collection<CatalogItem> catalogItems) {
    var map = new HashMap<CatalogItem, Element>();
    var query = genericElementRepository.query(unit.getClient());
    query.whereOwnerIs(unit);
    query.whereAppliedItemsContain(catalogItems);
    query.fetchAppliedCatalogItems();
    query
        .execute(PagingConfiguration.UNPAGED)
        .getResultPage()
        .forEach(e -> e.getAppliedCatalogItems().forEach(ci -> map.put(ci, e)));
    return map;
  }

  @Valid
  @Value
  public static class InputData implements UseCase.InputData {
    Client authenticatedClient;
    @NotNull Key<UUID> unitId;
    @NotNull List<Key<UUID>> catalogItemIds;
    IncarnationRequestModeType requestType;
    List<TailoringReferenceType> include;
    List<TailoringReferenceType> exclude;
  }

  @Valid
  @Value
  public static class OutputData implements UseCase.OutputData {
    @Valid List<TemplateItemIncarnationDescription> references;
    Unit container;
  }
}
