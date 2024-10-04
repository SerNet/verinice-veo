/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Jonas Jordan
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
import java.util.UUID;

import jakarta.validation.Valid;

import org.veo.core.entity.Key;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.entity.risk.DomainRiskReferenceProvider;
import org.veo.core.repository.DomainRepository;
import org.veo.core.repository.RepositoryProvider;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;
import org.veo.service.TemplateItemMigrationService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DeleteRiskDefinitionUseCase
    implements TransactionalUseCase<DeleteRiskDefinitionUseCase.InputData, UseCase.EmptyOutput> {

  private final DomainRepository domainRepository;
  private final RepositoryProvider repositoryProvider;
  private final TemplateItemMigrationService templateItemMigrationService;

  @Override
  public EmptyOutput execute(InputData input) {
    var domain = domainRepository.getById(input.domainId, input.authenticatedClientId);
    if (!domain.isActive()) {
      throw new NotFoundException("Domain is inactive.");
    }
    var riskDefRef =
        DomainRiskReferenceProvider.referencesForDomain(domain)
            .getRiskDefinitionRef(input.riskDefinitionRef);
    domain.removeRiskDefinition(riskDefRef);
    repositoryProvider.getRiskRelatedElementRepos().stream()
        .flatMap(r -> r.findByDomain(domain).stream())
        .forEach(
            e -> {
              if (e.removeRiskDefinition(riskDefRef, domain)) {
                e.setUpdatedAt(Instant.now());
              }
            });

    templateItemMigrationService.removeRiskDefinition(domain, riskDefRef);
    return EmptyOutput.INSTANCE;
  }

  @Override
  public boolean isReadOnly() {
    return false;
  }

  @Valid
  public record InputData(
      Key<UUID> authenticatedClientId, Key<UUID> domainId, String riskDefinitionRef)
      implements UseCase.InputData {}
}
