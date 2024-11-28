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

import javax.annotation.Nullable;

import org.veo.core.entity.Client;
import org.veo.core.entity.Control;
import org.veo.core.entity.Domain;
import org.veo.core.entity.RiskAffected;
import org.veo.core.entity.compliance.ControlImplementation;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.entity.exception.UnprocessableDataException;
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

  public enum ControlImplementationPurpose {
    MITIGATION,
    COMPLIANCE
  }

  @Override
  public OutputData execute(InputData input) {
    var query = controlImplementationRepository.query(input.authenticatedClient, input.domainId);
    Domain domain = domainRepository.getById(input.domainId, input.authenticatedClient.getId());
    applyAdditionalQueryParameters(input, domain, query);

    var result = query.execute(input.pagingConfiguration);
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
                    control, input.domainId.toString());
              }
              query.whereControlIdIn(id);
            });
    Optional.ofNullable(input.riskAffectedId)
        .ifPresent(
            id -> {
              RiskAffected<?, ?> ra =
                  genericElementRepository.getById(
                      id.getId(), id.getType(), input.authenticatedClient);
              if (!ra.isAssociatedWithDomain(domain)) {
                throw NotFoundException.elementNotAssociatedWithDomain(
                    ra, input.domainId.toString());
              }
              query.whereRiskAffectedIs(id.getId());
            });

    Optional.ofNullable(input.purpose)
        .ifPresent(
            cf -> {
              switch (cf) {
                case ControlImplementationPurpose.COMPLIANCE ->
                    Optional.ofNullable(
                            domain
                                .getControlImplementationConfiguration()
                                .complianceControlSubType())
                        .ifPresentOrElse(
                            subType -> query.whereControlhasSubType(subType, input.domainId),
                            () -> {
                              throw new UnprocessableDataException(
                                  "No compliance control sub type configured in domain.");
                            });
                case ControlImplementationPurpose.MITIGATION ->
                    Optional.ofNullable(
                            domain
                                .getControlImplementationConfiguration()
                                .mitigationControlSubType())
                        .ifPresentOrElse(
                            subType -> query.whereControlhasSubType(subType, input.domainId),
                            () -> {
                              throw new UnprocessableDataException(
                                  "No mitigation control sub type configured in domain.");
                            });
              }
            });
  }

  @Valid
  public record InputData(
      @NotNull Client authenticatedClient,
      UUID control,
      @NotNull UUID domainId,
      TypedId<? extends RiskAffected<?, ?>> riskAffectedId,
      @Nullable ControlImplementationPurpose purpose,
      @NotNull PagingConfiguration<String> pagingConfiguration)
      implements UseCase.InputData {}

  public record OutputData(@Valid PagedResult<ControlImplementation, String> page)
      implements UseCase.OutputData {}
}
