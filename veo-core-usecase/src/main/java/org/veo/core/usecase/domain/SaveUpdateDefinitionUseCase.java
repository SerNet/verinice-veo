/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2024  Urs Zeidler
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
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.veo.core.entity.Client;
import org.veo.core.entity.Key;
import org.veo.core.entity.definitions.DomainMigrationDefinition;
import org.veo.core.entity.definitions.DomainMigrationStep;
import org.veo.core.repository.DomainRepository;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SaveUpdateDefinitionUseCase
    implements TransactionalUseCase<SaveUpdateDefinitionUseCase.InputData, UseCase.EmptyOutput> {
  private final DomainRepository repository;

  @Override
  public EmptyOutput execute(InputData input) {
    var domain = repository.getActiveById(input.domainId, input.authenticatedClient.getId());

    var dmd = new DomainMigrationDefinition(input.migrationSteps());
    dmd.validate(domain);
    domain.setDomainMigrationDefinition(dmd);
    domain.setUpdatedAt(Instant.now());

    return EmptyOutput.INSTANCE;
  }

  @Override
  public boolean isReadOnly() {
    return false;
  }

  @Valid
  public record InputData(
      @NotNull Client authenticatedClient,
      @NotNull Key<UUID> domainId,
      @NotNull List<DomainMigrationStep> migrationSteps)
      implements UseCase.InputData {}
}
