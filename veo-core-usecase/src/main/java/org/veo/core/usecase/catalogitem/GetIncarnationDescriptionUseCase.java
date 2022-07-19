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

import javax.validation.Valid;

import org.veo.core.entity.CatalogItem;
import org.veo.core.entity.Client;
import org.veo.core.entity.DomainTemplate;
import org.veo.core.entity.Element;
import org.veo.core.entity.Identifiable;
import org.veo.core.entity.Key;
import org.veo.core.entity.LinkTailoringReference;
import org.veo.core.entity.TailoringReferenceTyped;
import org.veo.core.entity.Unit;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.entity.exception.RuntimeModelException;
import org.veo.core.entity.util.TailoringReferenceComparators;
import org.veo.core.repository.CatalogItemRepository;
import org.veo.core.repository.ElementQuery;
import org.veo.core.repository.ElementRepository;
import org.veo.core.repository.PagingConfiguration;
import org.veo.core.repository.UnitRepository;
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
public class GetIncarnationDescriptionUseCase
    implements TransactionalUseCase<
        GetIncarnationDescriptionUseCase.InputData, GetIncarnationDescriptionUseCase.OutputData> {
  private final UnitRepository unitRepository;
  private final CatalogItemRepository catalogItemRepository;
  private final org.veo.core.repository.RepositoryProvider repositoryProvider;

  @Override
  public OutputData execute(InputData input) {
    log.info("GetIncarnationDescriptionUseCase: {}", input);
    Unit unit =
        unitRepository
            .findByIdFetchClient(input.getContainerId())
            .orElseThrow(() -> new NotFoundException("Unit %s not found.", input.getContainerId()));
    unit.checkSameClient(input.authenticatedClient);
    validateInput(input);
    List<Key<UUID>> catalogItemIds = input.getCatalogItemIds();
    Map<Key<UUID>, CatalogItem> catalogItemsbyId =
        catalogItemRepository.getByIdsFetchElementData(Set.copyOf(catalogItemIds)).stream()
            .collect(Collectors.toMap(CatalogItem::getId, Function.identity()));
    List<CatalogItem> itemsToCreate =
        input.getCatalogItemIds().stream()
            .map(
                id -> {
                  CatalogItem catalogItem = catalogItemsbyId.get(id);
                  if (catalogItem == null) {
                    throw new NotFoundException("CatalogItem not found %s", id);
                  }
                  return catalogItem;
                })
            .flatMap(ci -> ci.getAllElementsToCreate().stream())
            .toList();

    Stream<CatalogItem> linkedCatalogItems =
        itemsToCreate.stream()
            .flatMap(
                catalogItem ->
                    catalogItem.getTailoringReferences().stream()
                        .filter(TailoringReferenceTyped.IS_ALL_LINK_PREDICATE)
                        .map(LinkTailoringReference.class::cast)
                        .map(LinkTailoringReference::getCatalogItem));

    Map<Key<UUID>, Element> referencedItemsByCatalogItemId = new HashMap<>();
    Map<Class<? extends Identifiable>, List<CatalogItem>> linkedItemsByElementType =
        linkedCatalogItems.collect(
            Collectors.groupingBy(item -> item.getElement().getModelInterface()));
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
    Set<DomainTemplate> usedDomains =
        itemsToCreate.stream()
            .map(ci -> ci.getCatalog().getDomainTemplate())
            .collect(Collectors.toSet());

    usedDomains.forEach(
        domain -> UseCaseTools.checkDomainBelongsToClient(input.getAuthenticatedClient(), domain));

    List<IncarnateCatalogItemDescription> incarnationDescriptions =
        itemsToCreate.stream()
            .map(
                catalogItem -> {
                  List<TailoringReferenceParameter> parameters =
                      toTailorreferenceParameters(catalogItem, referencedItemsByCatalogItemId);
                  return new IncarnateCatalogItemDescription(catalogItem, parameters);
                })
            .toList();
    log.info(
        "GetIncarnationDescriptionUseCase IncarnationDescription: {}", incarnationDescriptions);
    return new OutputData(incarnationDescriptions, unit);
  }

  private void validateInput(InputData input) {
    if (input.catalogItemIds.stream().collect(Collectors.toSet()).size()
        != input.catalogItemIds.size()) {
      throw new IllegalArgumentException("Provided catalogitems are not unique.");
    }
  }

  private List<TailoringReferenceParameter> toTailorreferenceParameters(
      CatalogItem catalogItem, Map<Key<UUID>, Element> referencedItemsByCatalogItemId) {
    return catalogItem.getTailoringReferences().stream()
        .filter(TailoringReferenceTyped.IS_ALL_LINK_PREDICATE)
        .sorted(TailoringReferenceComparators.BY_EXECUTION)
        .map(LinkTailoringReference.class::cast)
        .map(lr -> toParameter(lr, referencedItemsByCatalogItemId.get(lr.getCatalogItem().getId())))
        .toList();
  }

  /**
   * Create the parameter object for this {@link LinkTailoringReference} it also adds the suggestion
   * found by {@link #findReferencedAppliedItem(Unit, CatalogItem)} in the reference.
   */
  private TailoringReferenceParameter toParameter(
      LinkTailoringReference linkReference, Element element) {
    if (linkReference.getLinkType() == null) {
      throw new RuntimeModelException(
          "LinkType should not be null affected TailoringReferences: " + linkReference.getId());
    }
    TailoringReferenceParameter tailoringReferenceParameter =
        new TailoringReferenceParameter(
            linkReference.getReferenceType(), linkReference.getLinkType());
    if (element != null) {
      tailoringReferenceParameter.setReferencedElement(element);
    }
    return tailoringReferenceParameter;
  }

  /** Searches for {@link Element}s in the unit which have the given catalogItems applied. */
  private List<Element> findReferencedAppliedItems(
      Unit unit, Collection<CatalogItem> catalogItems) {
    Class<Element> entityType =
        (Class<Element>) catalogItems.iterator().next().getElement().getModelInterface();

    ElementRepository<Element> repository = repositoryProvider.getElementRepositoryFor(entityType);
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
    Key<UUID> containerId;
    List<Key<UUID>> catalogItemIds;
  }

  @Valid
  @Value
  public static class OutputData implements UseCase.OutputData {
    @Valid List<IncarnateCatalogItemDescription> references;
    Unit container;
  }
}
