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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.validation.Valid;

import org.veo.core.entity.CatalogItem;
import org.veo.core.entity.Client;
import org.veo.core.entity.DomainBase;
import org.veo.core.entity.Element;
import org.veo.core.entity.Identifiable;
import org.veo.core.entity.Key;
import org.veo.core.entity.LinkTailoringReference;
import org.veo.core.entity.TailoringReferenceTyped;
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
import org.veo.core.usecase.UseCaseTools;
import org.veo.core.usecase.parameter.TailoringReferenceParameter;
import org.veo.core.usecase.parameter.TemplateItemIncarnationDescription;

import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Slf4j
public class GetIncarnationDescriptionUseCase
    extends AbstractGetIncarnationDescriptionUseCase<CatalogItem>
    implements TransactionalUseCase<
        GetIncarnationDescriptionUseCase.InputData, GetIncarnationDescriptionUseCase.OutputData> {
  private final UnitRepository unitRepository;
  private final CatalogItemRepository catalogItemRepository;
  private final org.veo.core.repository.RepositoryProvider repositoryProvider;

  @Override
  public OutputData execute(InputData input) {
    log.info("unid: {}, items: {}", input.unitId, input.catalogItemIds);
    Unit unit = unitRepository.getByIdFetchClient(input.getUnitId());
    unit.checkSameClient(input.authenticatedClient);
    validateInput(input);
    List<Key<UUID>> catalogItemIds = input.getCatalogItemIds();
    Map<Key<UUID>, CatalogItem> catalogItemsbyId =
        catalogItemRepository
            .findAllByIdsFetchDomainAndTailoringReferences(Set.copyOf(catalogItemIds))
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
            .flatMap(ci -> ci.getAllElementsToCreate().stream())
            .distinct()
            .toList();
    IncarnationRequestModeType requestType =
        input.requestType == null ? IncarnationRequestModeType.DEFAULT : input.requestType;
    Collection<CatalogItem> itemsToCreate =
        switch (requestType) {
          case DEFAULT -> collectAllItems(requestedItems);
          case MANUAL -> requestedItems;
        };

    Stream<CatalogItem> linkedCatalogItems =
        itemsToCreate.stream()
            .flatMap(
                catalogItem ->
                    catalogItem.getTailoringReferences().stream()
                        .filter(TailoringReferenceTyped.IS_ALL_LINK_PREDICATE)
                        .map(tr -> (LinkTailoringReference<CatalogItem>) tr)
                        .map(LinkTailoringReference::getTarget));

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
    Set<DomainBase> usedDomains =
        itemsToCreate.stream().map(CatalogItem::getOwner).collect(Collectors.toSet());

    usedDomains.forEach(
        domain -> UseCaseTools.checkDomainBelongsToClient(input.getAuthenticatedClient(), domain));

    List<TemplateItemIncarnationDescription> incarnationDescriptions =
        itemsToCreate.stream()
            .map(
                catalogItem -> {
                  List<TailoringReferenceParameter> parameters =
                      toParameters(
                          catalogItem.getTailoringReferences(), referencedItemsByCatalogItemId);
                  return new TemplateItemIncarnationDescription(catalogItem, parameters);
                })
            .toList();
    log.info(
        "GetIncarnationDescriptionUseCase IncarnationDescription: {}", incarnationDescriptions);
    return new OutputData(incarnationDescriptions, unit);
  }

  /**
   * Collect recursively all link targets together with the elements until all targets are included.
   */
  private List<CatalogItem> collectAllItems(List<CatalogItem> itemsToCreate) {
    List<CatalogItem> current = itemsToCreate;
    List<CatalogItem> next =
        Stream.concat(
                itemsToCreate.stream(), itemsToCreate.stream().flatMap(this::getReferencedTargets))
            .distinct()
            .toList();
    while (current.size() < next.size()) {
      current = collectAllItems(next);
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
    Key<UUID> unitId;
    List<Key<UUID>> catalogItemIds;
    IncarnationRequestModeType requestType;
  }

  @Valid
  @Value
  public static class OutputData implements UseCase.OutputData {
    @Valid List<TemplateItemIncarnationDescription> references;
    Unit container;
  }
}
