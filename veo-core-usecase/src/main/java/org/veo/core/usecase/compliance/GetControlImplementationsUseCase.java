/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2024  Aziz Khalledi
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
package org.veo.core.usecase.compliance;

import java.util.Optional;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.veo.core.entity.Client;
import org.veo.core.entity.Control;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Key;
import org.veo.core.entity.RiskAffected;
import org.veo.core.entity.compliance.ControlImplementation;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.entity.ref.TypedId;
import org.veo.core.repository.ControlImplementationQuery;
import org.veo.core.repository.ControlImplementationRepository;
import org.veo.core.repository.DomainRepository;
import org.veo.core.repository.GenericElementRepository;
import org.veo.core.repository.PagedResult;
import org.veo.core.repository.PagingConfiguration;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;

import lombok.RequiredArgsConstructor;

/** Return all {@link ControlImplementation}s for the given {@link org.veo.core.entity.Control}. */
@RequiredArgsConstructor
public class GetControlImplementationsUseCase
    implements TransactionalUseCase<
        GetControlImplementationsUseCase.InputData, GetControlImplementationsUseCase.OutputData> {

  private final ControlImplementationRepository controlImplementationRepository;
  private final GenericElementRepository genericElementRepository;
  private final DomainRepository domainRepository;

  @Override
  public OutputData execute(InputData input) {
    var query = controlImplementationRepository.query(input.authenticatedClient, input.domainId);
    Domain domain = domainRepository.getById(input.domainId, input.authenticatedClient.getId());
    applyAdditionalQueryParameters(input, domain, query);
    PagedResult<ControlImplementation> result = query.execute(input.pagingConfiguration);
    return new OutputData(result);
  }

  private void applyAdditionalQueryParameters(
      InputData input, Domain domain, ControlImplementationQuery query) {
    Optional.ofNullable(input.control)
        .ifPresent(
            id -> {
              Control control =
                  genericElementRepository.getById(id, Control.class, input.authenticatedClient);
              if (!control.isAssociatedWithDomain(domain)) {
                throw NotFoundException.elementNotAssociatedWithDomain(
                    control, input.domainId.uuidValue());
              }
              query.whereControlIdIn(id);
            });
    Optional.ofNullable(input.riskAffectedId)
        .ifPresent(
            id -> {
              RiskAffected<?, ?> ra =
                  genericElementRepository.getById(
                      id.toKey(), id.getType(), input.authenticatedClient);
              if (!ra.isAssociatedWithDomain(domain)) {
                throw NotFoundException.elementNotAssociatedWithDomain(
                    ra, input.domainId.uuidValue());
              }
              query.whereRiskAffectedIs(id.toKey().value());
            });
  }

  @Valid
  public record InputData(
      @NotNull Client authenticatedClient,
      Key<UUID> control,
      @NotNull Key<UUID> domainId,
      TypedId<? extends RiskAffected<?, ?>> riskAffectedId,
      @NotNull PagingConfiguration pagingConfiguration)
      implements UseCase.InputData {}

  public record OutputData(@Valid PagedResult<ControlImplementation> page)
      implements UseCase.OutputData {}
}
