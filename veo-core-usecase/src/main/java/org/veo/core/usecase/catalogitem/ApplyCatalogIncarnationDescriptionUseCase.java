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

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.veo.core.UserAccessRights;
import org.veo.core.entity.CatalogItem;
import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainBase;
import org.veo.core.entity.Element;
import org.veo.core.entity.Unit;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.entity.exception.UnprocessableDataException;
import org.veo.core.entity.state.TemplateItemIncarnationDescriptionState;
import org.veo.core.repository.CatalogItemRepository;
import org.veo.core.repository.ClientRepository;
import org.veo.core.repository.DomainRepository;
import org.veo.core.repository.UnitRepository;
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
  private final DomainRepository domainRepository;
  private final UnitRepository unitRepository;
  private final ClientRepository clientRepository;

  @Override
  public OutputData execute(InputData input, UserAccessRights userAccessRights) {

    var client = clientRepository.getById(userAccessRights.getClientId());
    var unit =
        unitRepository
            .findById(input.unitId, userAccessRights)
            .orElseThrow(() -> new NotFoundException(input.unitId(), Unit.class));
    userAccessRights.checkElementWriteAccess(unit);
    if (input.descriptions.isEmpty()) {
      return new OutputData(Collections.emptyList(), null);
    }
    var domain = getDomain(input.descriptions, client);
    return new OutputData(
        applier.incarnate(unit, input.descriptions, catalogItemRepository, client), domain);
  }

  private Domain getDomain(
      @NotNull List<TemplateItemIncarnationDescriptionState<CatalogItem, DomainBase>> descriptions,
      Client authenticatedClient) {
    var domainIds =
        descriptions.stream().map(d -> d.getItemRef().getNamespaceId()).distinct().toList();
    if (domainIds.size() > 1) {
      throw new UnprocessableDataException(
          "Cannot apply incarnation descriptions for multiple domains in a single request.");
    }
    return domainRepository.getActiveById(domainIds.getFirst(), authenticatedClient.getId());
  }

  @Override
  public boolean isReadOnly() {
    return false;
  }

  @Valid
  public record InputData(
      @NotNull UUID unitId,
      @NotNull List<TemplateItemIncarnationDescriptionState<CatalogItem, DomainBase>> descriptions)
      implements UseCase.InputData {}

  @Valid
  public record OutputData(@Valid List<Element> newElements, Domain domain)
      implements UseCase.OutputData {}
}
