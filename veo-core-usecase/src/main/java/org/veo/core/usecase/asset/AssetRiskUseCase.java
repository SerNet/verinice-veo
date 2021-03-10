/*******************************************************************************
 * Copyright (c) 2020 Alexander Koderman.
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.veo.core.usecase.asset;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.validation.Valid;

import org.veo.core.entity.AssetRisk;
import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Key;
import org.veo.core.entity.specification.ClientBoundaryViolationException;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.repository.AssetRepository;
import org.veo.core.usecase.repository.ControlRepository;
import org.veo.core.usecase.repository.DomainRepository;
import org.veo.core.usecase.repository.PersonRepository;
import org.veo.core.usecase.repository.ScenarioRepository;

import lombok.AllArgsConstructor;
import lombok.Value;

public abstract class AssetRiskUseCase
        implements UseCase<AssetRiskUseCase.InputData, AssetRiskUseCase.OutputData> {

    protected final AssetRepository assetRepository;
    protected final ControlRepository controlRepository;
    protected final PersonRepository personRepository;
    protected final ScenarioRepository scenarioRepository;
    protected final DomainRepository domainRepository;

    public AssetRiskUseCase(AssetRepository assetRepository, ControlRepository controlRepository,
            PersonRepository personRepository, ScenarioRepository scenarioRepository,
            DomainRepository domainRepository) {
        this.assetRepository = assetRepository;
        this.controlRepository = controlRepository;
        this.personRepository = personRepository;
        this.scenarioRepository = scenarioRepository;
        this.domainRepository = domainRepository;
    }

    protected AssetRisk applyOptionalInput(InputData input, AssetRisk risk) {
        var updatedRisk = risk;

        if (input.getControlRef()
                 .isPresent()) {
            var control = controlRepository.findById(input.getControlRef()
                                                          .get())
                                           .orElseThrow();
            control.checkSameClient(input.getAuthenticatedClient());
            updatedRisk = updatedRisk.mitigate(control);
        }

        if (input.getRiskOwnerRef()
                 .isPresent()) {
            var riskOwner = personRepository.findById(input.getRiskOwnerRef()
                                                           .get())
                                            .orElseThrow();
            riskOwner.checkSameClient(input.getAuthenticatedClient());
            updatedRisk = updatedRisk.appoint(riskOwner);
        }

        return updatedRisk;
    }

    protected void checkClients(Client authenticatedClient, Set<Domain> domains) {
        if (domains.stream()
                   .anyMatch(domain -> !domain.getOwner()
                                              .equals(authenticatedClient)))
            throw new ClientBoundaryViolationException("Illegal client for attempted operation.");
    }

    @Valid
    @Value
    @AllArgsConstructor
    public static class InputData implements UseCase.InputData {
        Client authenticatedClient;
        Key<UUID> assetRef;
        Key<UUID> scenarioRef;
        Set<Key<UUID>> domainRefs;
        @Nullable
        Key<UUID> controlRef;
        @Nullable
        Key<UUID> riskOwnerRef;

        @Nullable
        String eTag;

        public InputData(Client authenticatedClient, Key<UUID> assetRef, Key<UUID> scenarioRef,
                Set<Key<UUID>> domainRefs, Key<UUID> controlRef, Key<UUID> riskOwnerRef) {
            this(authenticatedClient, assetRef, scenarioRef, domainRefs, controlRef, riskOwnerRef,
                    null);
        }

        public Optional<Key<UUID>> getControlRef() {
            return Optional.ofNullable(controlRef);
        }

        public Optional<Key<UUID>> getRiskOwnerRef() {
            return Optional.ofNullable(riskOwnerRef);
        }
    }

    @Valid
    @Value
    public static class OutputData implements UseCase.OutputData {
        @Valid
        AssetRisk assetRisk;
    }
}
