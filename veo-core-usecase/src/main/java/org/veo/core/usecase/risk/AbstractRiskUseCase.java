/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Alexander Koderman.
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
package org.veo.core.usecase.risk;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import javax.annotation.Nullable;

import org.veo.core.UserAccessRights;
import org.veo.core.entity.AbstractRisk;
import org.veo.core.entity.Client;
import org.veo.core.entity.Control;
import org.veo.core.entity.Person;
import org.veo.core.entity.RiskAffected;
import org.veo.core.entity.risk.RiskValues;
import org.veo.core.repository.RepositoryProvider;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.base.AbstractUseCase;

import lombok.AllArgsConstructor;
import lombok.Value;

public abstract class AbstractRiskUseCase<
        T extends RiskAffected<T, R>, R extends AbstractRisk<T, R>>
    extends AbstractUseCase<AbstractRiskUseCase.InputData, AbstractRiskUseCase.OutputData<R>> {

  public AbstractRiskUseCase(RepositoryProvider repositoryProvider) {
    super(repositoryProvider);
  }

  protected R applyOptionalInput(InputData input, R risk) {
    if (input.getControlRef().isPresent()) {
      var control =
          repositoryProvider
              .getElementRepositoryFor(Control.class)
              .findById(input.getControlRef().get(), input.user)
              .orElseThrow();
      control.checkSameClient(input.authenticatedClient());
      risk.mitigate(control);
    }

    if (input.getRiskOwnerRef().isPresent()) {
      var riskOwner =
          repositoryProvider
              .getElementRepositoryFor(Person.class)
              .findById(input.getRiskOwnerRef().get(), input.user)
              .orElseThrow();
      riskOwner.checkSameClient(input.authenticatedClient());
      risk.appoint(riskOwner);
    }

    return risk;
  }

  @Valid
  public record InputData(
      @NotNull UserAccessRights user,
      @NotNull Client authenticatedClient,
      @NotNull UUID riskAffectedRef,
      @NotNull UUID scenarioRef,
      @NotNull Set<@NotNull UUID> domainRefs,
      @Nullable UUID controlRef,
      @Nullable UUID riskOwnerRef,
      @Nullable String eTag,
      Set<RiskValues> riskValues)
      implements UseCase.InputData {

    public Optional<UUID> getControlRef() {
      return Optional.ofNullable(controlRef);
    }

    public Optional<UUID> getRiskOwnerRef() {
      return Optional.ofNullable(riskOwnerRef);
    }

    public InputData(
        UserAccessRights user,
        Client authenticatedClient,
        UUID riskAffectedRef,
        UUID scenarioRef,
        Set<UUID> domainRefs,
        UUID controlRef,
        UUID riskOwnerRef,
        Set<RiskValues> riskValues) {
      this(
          user,
          authenticatedClient,
          riskAffectedRef,
          scenarioRef,
          domainRefs,
          controlRef,
          riskOwnerRef,
          null,
          riskValues);
    }
  }

  @Valid
  @Value
  @AllArgsConstructor
  public static class OutputData<R extends AbstractRisk<?, ?>> implements UseCase.OutputData {
    @Valid R risk;

    boolean newlyCreatedRisk;

    public OutputData(R risk) {
      this.risk = risk;
      this.newlyCreatedRisk = false;
    }
  }
}
