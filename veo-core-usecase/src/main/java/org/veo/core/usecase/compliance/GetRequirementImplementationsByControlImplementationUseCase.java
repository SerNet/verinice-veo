/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Alexander Koderman
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

import java.util.stream.Collectors;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import javax.annotation.Nullable;

import org.veo.core.entity.Client;
import org.veo.core.entity.Control;
import org.veo.core.entity.Domain;
import org.veo.core.entity.RiskAffected;
import org.veo.core.entity.compliance.ControlImplementation;
import org.veo.core.entity.compliance.ReqImplRef;
import org.veo.core.entity.compliance.RequirementImplementation;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.entity.ref.ITypedId;
import org.veo.core.repository.PagedResult;
import org.veo.core.repository.PagingConfiguration;
import org.veo.core.repository.QueryCondition;
import org.veo.core.repository.RepositoryProvider;
import org.veo.core.repository.RequirementImplementationRepository;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.base.AbstractUseCase;

/** Return all {@link RequirementImplementation}s for the given {@link ControlImplementation}. */
public class GetRequirementImplementationsByControlImplementationUseCase
    extends AbstractUseCase<
        GetRequirementImplementationsByControlImplementationUseCase.InputData,
        GetRequirementImplementationsByControlImplementationUseCase.OutputData> {

  private final RequirementImplementationRepository requirementImplementationRepository;

  public GetRequirementImplementationsByControlImplementationUseCase(
      RepositoryProvider repositoryProvider,
      RequirementImplementationRepository requirementImplementationRepository) {
    super(repositoryProvider);
    this.requirementImplementationRepository = requirementImplementationRepository;
  }

  @Override
  public OutputData execute(InputData input) {
    var owner = getEntity(input.owner, input.authenticatedClient);
    var control = getEntity(input.control, input.authenticatedClient);
    var implementation = owner.getImplementationFor(control);
    var riIds =
        implementation.getRequirementImplementations().stream()
            .map(ReqImplRef::getUUID)
            .collect(Collectors.toSet());

    var query = requirementImplementationRepository.query(input.authenticatedClient);
    var domain = input.domain == null ? null : getEntity(input.domain, input.authenticatedClient);
    if (domain != null) {
      if (!domain.isActive()) {
        throw new NotFoundException("Domain is inactive.");
      }
      if (!control.isAssociatedWithDomain(domain)) {
        throw NotFoundException.elementNotAssociatedWithDomain(
            control, input.domain.getId().toString());
      }
      if (!owner.isAssociatedWithDomain(domain))
        throw NotFoundException.elementNotAssociatedWithDomain(
            owner, input.domain.getId().toString());
      query.whereControlInDomain(domain);
    }

    query.whereIdsIn(new QueryCondition<>(riIds));
    return new OutputData(query.execute(input.pagingConfiguration), domain);
  }

  @Valid
  public record InputData(
      @NotNull Client authenticatedClient,
      @NotNull ITypedId<? extends RiskAffected<?, ?>> owner,
      @NotNull ITypedId<Control> control,
      @Nullable ITypedId<Domain> domain,
      @NotNull PagingConfiguration<String> pagingConfiguration)
      implements UseCase.InputData {

    public InputData(
        @NotNull Client authenticatedClient,
        @NotNull ITypedId<? extends RiskAffected<?, ?>> owner,
        @NotNull ITypedId<Control> control,
        @NotNull PagingConfiguration<String> pagingConfiguration) {
      this(authenticatedClient, owner, control, null, pagingConfiguration);
    }
  }

  @Valid
  public record OutputData(
      @NotNull PagedResult<RequirementImplementation, String> result, Domain domain)
      implements UseCase.OutputData {}
}
