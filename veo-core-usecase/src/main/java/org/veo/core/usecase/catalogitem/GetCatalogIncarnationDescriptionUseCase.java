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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.veo.core.entity.CatalogItem;
import org.veo.core.entity.Client;
import org.veo.core.entity.DomainBase;
import org.veo.core.entity.IncarnationLookup;
import org.veo.core.entity.IncarnationRequestModeType;
import org.veo.core.entity.TailoringReferenceType;
import org.veo.core.entity.Unit;
import org.veo.core.repository.CatalogItemRepository;
import org.veo.core.repository.DomainRepository;
import org.veo.core.repository.GenericElementRepository;
import org.veo.core.repository.UnitRepository;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.parameter.TemplateItemIncarnationDescription;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GetCatalogIncarnationDescriptionUseCase
    extends AbstractGetIncarnationDescriptionUseCase<CatalogItem, DomainBase>
    implements TransactionalUseCase<
        GetCatalogIncarnationDescriptionUseCase.InputData,
        GetCatalogIncarnationDescriptionUseCase.OutputData> {

  private final DomainRepository domainRepository;
  private final UnitRepository unitRepository;
  private final CatalogItemRepository catalogItemRepository;

  public GetCatalogIncarnationDescriptionUseCase(
      DomainRepository domainRepository,
      UnitRepository unitRepository,
      CatalogItemRepository catalogItemRepository,
      GenericElementRepository genericElementRepository) {
    super(CatalogItem.class, genericElementRepository);
    this.domainRepository = domainRepository;
    this.unitRepository = unitRepository;
    this.catalogItemRepository = catalogItemRepository;
  }

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
    var items =
        catalogItemRepository.findAllByIdsFetchTailoringReferences(input.catalogItemIds(), domain);
    var incarnationDescriptions =
        getIncarnationDescriptions(
            input.catalogItemIds(),
            items,
            domain,
            domain,
            unit,
            input.requestType,
            input.lookup,
            input.include,
            input.exclude,
            false);
    log.info("incarnation descriptions: {}", incarnationDescriptions);
    return new OutputData(incarnationDescriptions, unit);
  }

  private void validateInput(InputData input) {
    if (new HashSet<>(input.catalogItemIds).size() != input.catalogItemIds.size()) {
      throw new IllegalArgumentException("Provided catalog items are not unique.");
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
