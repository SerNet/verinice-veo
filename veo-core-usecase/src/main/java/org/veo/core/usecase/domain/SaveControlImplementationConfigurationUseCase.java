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
import org.veo.core.entity.ControlImplementationConfigurationDto;
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
    var config =
        input.controlImplementationConfiguration.toConfig(
            domain.getControlImplementationConfiguration());
    validate(domain.getElementTypeDefinition(Control.SINGULAR_TERM), config);
    domain.setControlImplementationConfiguration(config);
    domain.setUpdatedAt(Instant.now());
    return EmptyOutput.INSTANCE;
  }

  private void validate(
      ElementTypeDefinition elementTypeDefinition,
      @NotNull ControlImplementationConfiguration controlImplementationConfiguration) {
    controlImplementationConfiguration
        .complianceControlSubTypes()
        .forEach(elementTypeDefinition::getSubTypeDefinition);
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
      @NotNull UUID domainId,
      // TODO #3860 use ControlImplementationConfiguration type again
      @NotNull ControlImplementationConfigurationDto controlImplementationConfiguration)
      implements UseCase.InputData {}
}
