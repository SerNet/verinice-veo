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

import javax.annotation.Nullable;
import javax.validation.Valid;

import org.veo.core.entity.AbstractRisk;
import org.veo.core.entity.Client;
import org.veo.core.entity.Control;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Key;
import org.veo.core.entity.Person;
import org.veo.core.entity.RiskAffected;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.entity.risk.RiskDefinitionRef;
import org.veo.core.entity.risk.RiskValues;
import org.veo.core.repository.RepositoryProvider;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.base.AbstractUseCase;

import lombok.AllArgsConstructor;
import lombok.Value;

public abstract class AbstractRiskUseCase<T extends RiskAffected<T, R>, R extends AbstractRisk<T, R>>
        extends AbstractUseCase<AbstractRiskUseCase.InputData, AbstractRiskUseCase.OutputData<R>> {

    public AbstractRiskUseCase(RepositoryProvider repositoryProvider) {
        super(repositoryProvider);
    }

    protected R applyOptionalInput(InputData input, R risk) {
        if (input.getControlRef()
                 .isPresent()) {
            var control = repositoryProvider.getRepositoryFor(Control.class)
                                            .findById(input.getControlRef()
                                                           .get())
                                            .orElseThrow();
            control.checkSameClient(input.getAuthenticatedClient());
            risk.mitigate(control);
        }

        if (input.getRiskOwnerRef()
                 .isPresent()) {
            var riskOwner = repositoryProvider.getRepositoryFor(Person.class)
                                              .findById(input.getRiskOwnerRef()
                                                             .get())
                                              .orElseThrow();
            riskOwner.checkSameClient(input.getAuthenticatedClient());
            risk.appoint(riskOwner);
        }

        return risk;
    }

    protected Domain domainForKey(Set<Domain> domains, Key<UUID> key) {
        return domains.stream()
                      .filter(d -> d.getId()
                                    .equals(key))
                      .findFirst()
                      .orElseThrow(() -> new NotFoundException(
                              "Could not resolve domain with ID %s", key.uuidValue()));
    }

    protected void validateRiskValues(Set<RiskValues> riskValues, Set<Domain> domains,
            T riskAffected) {
        if (riskValues == null) {
            return;
        }
        riskValues.forEach(rv -> {
            var domain = domains.stream()
                                .filter(d -> d.getId()
                                              .equals(rv.getDomainId()))
                                .findAny()
                                .orElseThrow();
            var riskDefinitionRef = domain.getRiskDefinition(rv.getRiskDefinitionId())
                                          .map(RiskDefinitionRef::from)
                                          .orElseThrow();
            validateRiskDefinition(riskAffected, riskDefinitionRef, domain);
        });
    }

    abstract protected void validateRiskDefinition(T riskAffected,
            RiskDefinitionRef riskDefinitionRef, Domain domain);

    @Valid
    @Value
    @AllArgsConstructor
    public static class InputData implements UseCase.InputData {
        Client authenticatedClient;
        Key<UUID> riskAffectedRef;
        Key<UUID> scenarioRef;
        Set<Key<UUID>> domainRefs;
        @Nullable
        Key<UUID> controlRef;
        @Nullable
        Key<UUID> riskOwnerRef;

        @Nullable
        String eTag;

        Set<RiskValues> riskValues;

        public Optional<Key<UUID>> getControlRef() {
            return Optional.ofNullable(controlRef);
        }

        public Optional<Key<UUID>> getRiskOwnerRef() {
            return Optional.ofNullable(riskOwnerRef);
        }

        public InputData(Client authenticatedClient, Key<UUID> riskAffectedRef,
                Key<UUID> scenarioRef, Set<Key<UUID>> domainRefs, Key<UUID> controlRef,
                Key<UUID> riskOwnerRef, Set<RiskValues> riskValues) {
            this(authenticatedClient, riskAffectedRef, scenarioRef, domainRefs, controlRef,
                    riskOwnerRef, null, riskValues);

        }
    }

    @Valid
    @Value
    @AllArgsConstructor
    public static class OutputData<R extends AbstractRisk<?, ?>> implements UseCase.OutputData {
        @Valid
        R risk;

        boolean newlyCreatedRisk;

        public OutputData(R risk) {
            this.risk = risk;
            this.newlyCreatedRisk = false;
        }
    }

}
