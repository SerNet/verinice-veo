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
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import org.veo.core.entity.Identifiable;
import org.veo.core.entity.Key;
import org.veo.core.entity.TailoringReference;
import org.veo.core.entity.TailoringReferenceType;
import org.veo.core.entity.TailoringReferenceTyped;
import org.veo.core.entity.TemplateItem;
import org.veo.core.entity.TemplateItemReference;
import org.veo.core.entity.Unit;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.repository.CatalogItemRepository;
import org.veo.core.repository.ElementQuery;
import org.veo.core.repository.ElementRepository;
import org.veo.core.repository.PagingConfiguration;
import org.veo.core.repository.UnitRepository;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.parameter.TailoringReferenceParameter;
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
  private final org.veo.core.repository.RepositoryProvider repositoryProvider;

  @Override
  public OutputData execute(InputData input) {
    log.info("unid: {}, items: {}", input.unitId, input.catalogItemIds);
    Unit unit = unitRepository.getByIdFetchClient(input.getUnitId());
    unit.checkSameClient(input.authenticatedClient);
    validateInput(input);

    Predicate<TailoringReferenceTyped> tailoringReferenceFilter =
        createTailoringReferenceFilter(input.exclude, input.include);
    List<Key<UUID>> catalogItemIds = input.getCatalogItemIds();
    Map<Key<UUID>, CatalogItem> catalogItemsbyId =
        catalogItemRepository
            .findAllByIdsFetchDomainAndTailoringReferences(
                Set.copyOf(catalogItemIds), input.authenticatedClient)
            .stream()
            .collect(Collectors.toMap(CatalogItem::getId, Function.identity()));
    List<CatalogItem> requestedItems =
        input.getCatalogItemIds().stream()
            .map(
                id -> {
                  CatalogItem catalogItem = catalogItemsbyId.get(id);
                  if (catalogItem == null) {
                    throw new NotFoundException(id, CatalogItem.class);
                  }
                  return catalogItem;
                })
            .flatMap(ci -> ci.getAllItemsToIncarnate().stream())
            .distinct()
            .toList();
    IncarnationRequestModeType requestType =
        input.requestType == null ? IncarnationRequestModeType.DEFAULT : input.requestType;
    Collection<CatalogItem> itemsToCreate =
        switch (requestType) {
          case DEFAULT -> collectAllItems(requestedItems, tailoringReferenceFilter);
          case MANUAL -> requestedItems;
        };

    Stream<CatalogItem> linkedCatalogItems =
        itemsToCreate.stream()
            .flatMap(
                catalogItem ->
                    catalogItem.getTailoringReferences().stream()
                        .filter(tailoringReferenceFilter)
                        .filter(TailoringReferenceTyped.IS_PARAMETER_REF)
                        .map(TailoringReference::getTarget));

    Map<Key<UUID>, Element> referencedItemsByCatalogItemId = new HashMap<>();
    Map<Class<? extends Identifiable>, List<CatalogItem>> linkedItemsByElementType =
        linkedCatalogItems.collect(Collectors.groupingBy(item -> item.getElementInterface()));
    linkedItemsByElementType.forEach(
        (elementType, items) ->
            findReferencedAppliedItems(unit, items)
                .forEach(
                    element ->
                        element
                            .getAppliedCatalogItems()
                            .forEach(
                                appliedItem ->
                                    referencedItemsByCatalogItemId.put(
                                        appliedItem.getId(), element))));

    List<TemplateItemIncarnationDescription> incarnationDescriptions =
        itemsToCreate.stream()
            .map(
                catalogItem -> {
                  List<TailoringReferenceParameter> parameters =
                      toParameters(
                          catalogItem.getTailoringReferences().stream()
                              .filter(tailoringReferenceFilter)
                              .toList(),
                          referencedItemsByCatalogItemId);
                  return new TemplateItemIncarnationDescription(catalogItem, parameters);
                })
            .toList();
    log.info(
        "GetIncarnationDescriptionUseCase IncarnationDescription: {}", incarnationDescriptions);
    return new OutputData(incarnationDescriptions, unit);
  }

  /**
   * Collect recursively all link targets together with the elements until all targets are included.
   *
   * @param tailoringReferenceFilter
   */
  private List<CatalogItem> collectAllItems(
      List<CatalogItem> itemsToCreate,
      Predicate<? super TailoringReference<CatalogItem>> tailoringReferenceFilter) {
    List<CatalogItem> current = itemsToCreate;
    List<CatalogItem> next =
        Stream.concat(
                itemsToCreate.stream(),
                itemsToCreate.stream()
                    .map(TemplateItem::getTailoringReferences)
                    .flatMap(Collection::stream)
                    .filter(tailoringReferenceFilter)
                    .map(tr -> tr.getTarget()))
            .distinct()
            .toList();
    while (current.size() < next.size()) {
      current = collectAllItems(next, tailoringReferenceFilter);
    }
    return current;
  }

  private Stream<? extends CatalogItem> getReferencedTargets(CatalogItem ci) {
    return ci.getTailoringReferences().stream().map(TemplateItemReference<CatalogItem>::getTarget);
  }

  private void validateInput(InputData input) {
    if (input.catalogItemIds.stream().collect(Collectors.toSet()).size()
        != input.catalogItemIds.size()) {
      throw new IllegalArgumentException("Provided catalogitems are not unique.");
    }
  }

  /** Searches for {@link Element}s in the unit which have the given catalogItems applied. */
  private List<Element> findReferencedAppliedItems(
      Unit unit, Collection<CatalogItem> catalogItems) {
    Set<Class<? extends Element>> types =
        catalogItems.stream().map(ci -> ci.getElementInterface()).collect(Collectors.toSet());
    if (types.size() != 1) {
      log.warn("more than one type as referenced element");
      // TODO: veo-2218 throw
    }

    ElementRepository<Element> repository =
        repositoryProvider.getElementRepositoryFor((Class<Element>) types.iterator().next());
    ElementQuery<Element> query = repository.query(unit.getClient());
    query.whereOwnerIs(unit);
    query.whereAppliedItemsContain(catalogItems);
    query.fetchAppliedCatalogItems();
    return query.execute(PagingConfiguration.UNPAGED).getResultPage();
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
