/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2024  Jonas Jordan
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
package org.veo.core.usecase.domain;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.veo.core.entity.Client;
import org.veo.core.entity.Control;
import org.veo.core.entity.ControlImplementationConfiguration;
import org.veo.core.entity.Key;
import org.veo.core.entity.definitions.ElementTypeDefinition;
import org.veo.core.repository.DomainRepository;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SaveControlImplementationConfigurationUseCase
    implements TransactionalUseCase<
        SaveControlImplementationConfigurationUseCase.InputData, UseCase.EmptyOutput> {

  private final DomainRepository domainRepository;

  @Override
  public EmptyOutput execute(InputData input) {
    var domain = domainRepository.getActiveById(input.domainId, input.authenticatedClient.getId());
    validate(
        domain.getElementTypeDefinition(Control.SINGULAR_TERM),
        input.controlImplementationConfiguration);
    domain.setControlImplementationConfiguration(input.controlImplementationConfiguration);
    domain.setUpdatedAt(Instant.now());
    return EmptyOutput.INSTANCE;
  }

  private void validate(
      ElementTypeDefinition elementTypeDefinition,
      @NotNull ControlImplementationConfiguration controlImplementationConfiguration) {
    Optional.ofNullable(controlImplementationConfiguration.complianceControlSubType())
        .ifPresent(elementTypeDefinition::getSubTypeDefinition);
    Optional.ofNullable(controlImplementationConfiguration.mitigationControlSubType())
        .ifPresent(elementTypeDefinition::getSubTypeDefinition);
  }

  @Override
  public boolean isReadOnly() {
    return false;
  }

  @Valid
  public record InputData(
      @NotNull Client authenticatedClient,
      @NotNull Key<UUID> domainId,
      @NotNull ControlImplementationConfiguration controlImplementationConfiguration)
      implements UseCase.InputData {}
}