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
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.veo.core.entity.CatalogItem;
import org.veo.core.entity.Client;
import org.veo.core.entity.DomainBase;
import org.veo.core.entity.Element;
import org.veo.core.entity.Key;
import org.veo.core.entity.state.TemplateItemIncarnationDescriptionState;
import org.veo.core.repository.CatalogItemRepository;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.parameter.TemplateItemIncarnationDescription;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Uses a list of {@link TemplateItemIncarnationDescription} to create items from a catalog in a
 * unit.
 */
@RequiredArgsConstructor
@Slf4j
public class ApplyCatalogIncarnationDescriptionUseCase
    implements TransactionalUseCase<
        ApplyCatalogIncarnationDescriptionUseCase.InputData,
        ApplyCatalogIncarnationDescriptionUseCase.OutputData> {
  private final CatalogItemRepository catalogItemRepository;
  private final IncarnationDescriptionApplier applier;

  @Override
  public OutputData execute(InputData input) {
    return new OutputData(
        applier.incarnate(
            input.unitId, input.descriptions, catalogItemRepository, input.authenticatedClient));
  }

  @Override
  public boolean isReadOnly() {
    return false;
  }

  @Valid
  public record InputData(
      Client authenticatedClient,
      @NotNull Key<UUID> unitId,
      @NotNull List<TemplateItemIncarnationDescriptionState<CatalogItem, DomainBase>> descriptions)
      implements UseCase.InputData {}

  @Valid
  public record OutputData(@Valid List<Element> newElements) implements UseCase.OutputData {}
}
