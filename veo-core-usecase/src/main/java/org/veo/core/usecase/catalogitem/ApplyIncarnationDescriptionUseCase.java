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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import jakarta.validation.Valid;

import org.veo.core.entity.CatalogItem;
import org.veo.core.entity.Client;
import org.veo.core.entity.Element;
import org.veo.core.entity.Identifiable;
import org.veo.core.entity.Key;
import org.veo.core.entity.Unit;
import org.veo.core.entity.exception.ReferenceTargetNotFoundException;
import org.veo.core.entity.transform.EntityFactory;
import org.veo.core.repository.CatalogItemRepository;
import org.veo.core.repository.DomainRepository;
import org.veo.core.repository.RepositoryProvider;
import org.veo.core.repository.UnitRepository;
import org.veo.core.usecase.DesignatorService;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.parameter.TemplateItemIncarnationDescription;

import lombok.Value;
import lombok.extern.slf4j.Slf4j;

/**
 * Uses a list of {@link TemplateItemIncarnationDescription} to create items from a catalog in a
 * unit.
 */
@Slf4j
public class ApplyIncarnationDescriptionUseCase
    extends AbtractApplyIncarnationDescriptionUseCase<CatalogItem>
    implements TransactionalUseCase<
        ApplyIncarnationDescriptionUseCase.InputData,
        ApplyIncarnationDescriptionUseCase.OutputData> {
  public ApplyIncarnationDescriptionUseCase(
      UnitRepository unitRepository,
      CatalogItemRepository catalogItemRepository,
      DomainRepository domainRepository,
      RepositoryProvider repositoryProvider,
      DesignatorService designatorService,
      EntityFactory factory) {
    super(designatorService, factory, domainRepository, unitRepository, repositoryProvider);
    this.catalogItemRepository = catalogItemRepository;
  }

  private final CatalogItemRepository catalogItemRepository;

  @Override
  public OutputData execute(InputData input) {
    // TODO: verinice-veo#2357 refactor this usecase
    log.info("ApplyIncarnationDescriptionUseCase: {}", input);
    Unit unit = unitRepository.getByIdFetchClient(input.getUnitId());
    Client authenticatedClient = input.authenticatedClient;
    unit.checkSameClient(authenticatedClient);
    Set<Key<UUID>> catalogItemIds =
        input.getReferencesToApply().stream()
            .map(TemplateItemIncarnationDescription::getItem)
            .map(Identifiable::getId)
            .collect(Collectors.toSet());
    Map<Key<UUID>, CatalogItem> catalogItemsbyId =
        catalogItemRepository.findAllByIdsFetchDomainAndTailoringReferences(catalogItemIds).stream()
            .collect(Collectors.toMap(CatalogItem::getId, Function.identity()));
    checkDomains(input.getAuthenticatedClient(), catalogItemsbyId);

    List<Element> createdElements =
        input.getReferencesToApply().stream()
            .map(ra -> incarnateByDescription(unit, authenticatedClient, catalogItemsbyId, ra))
            .collect(
                Collector.of(
                    IncarnationResult::new,
                    collectElementData(),
                    combineElementData(),
                    elementData -> {
                      processInternalLinks(
                          elementData.getInternalLinks(), elementData.getElements());
                      processParts(elementData.getMapping(), elementData.getInternalLinks());
                      return elementData.getElements();
                    }));
    log.info("ApplyIncarnationDescriptionUseCase elements created: {}", createdElements);
    return new ApplyIncarnationDescriptionUseCase.OutputData(createdElements);
  }

  protected ElementResult<CatalogItem> incarnateByDescription(
      Unit unit,
      Client authenticatedClient,
      Map<Key<UUID>, CatalogItem> catalogItemsbyId,
      TemplateItemIncarnationDescription ra) {
    Key<UUID> catalogItemId = ra.getItem().getId();
    CatalogItem catalogItem = catalogItemsbyId.get(catalogItemId);
    if (catalogItem == null) {
      throw new ReferenceTargetNotFoundException(catalogItemId, CatalogItem.class);
    }
    return createElementFromCatalogItem(
        unit,
        authenticatedClient,
        catalogItem,
        catalogItem.getTailoringReferences(),
        catalogItem.requireDomainMembership(),
        ra.getReferences());
  }

  @Override
  public boolean isReadOnly() {
    return false;
  }

  @Valid
  @Value
  public static class InputData implements UseCase.InputData {
    Client authenticatedClient;
    Key<UUID> unitId;
    List<TemplateItemIncarnationDescription> referencesToApply;
  }

  @Valid
  @Value
  public static class OutputData implements UseCase.OutputData {
    @Valid List<Element> newElements;
  }
}
