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
package org.veo.core.usecase.process;

import org.veo.core.entity.Domain;
import org.veo.core.entity.Process;
import org.veo.core.entity.ProcessRisk;
import org.veo.core.entity.risk.RiskDefinitionRef;
import org.veo.core.repository.RepositoryProvider;
import org.veo.core.service.EventPublisher;
import org.veo.core.usecase.DesignatorService;
import org.veo.core.usecase.base.ScopeProvider;
import org.veo.core.usecase.risk.CreateRiskUseCase;

public class CreateProcessRiskUseCase extends CreateRiskUseCase<Process, ProcessRisk> {
  private final ScopeProvider scopeProvider;

  public CreateProcessRiskUseCase(
      RepositoryProvider repositoryProvider,
      DesignatorService designatorService,
      EventPublisher eventPublisher,
      ScopeProvider scopeProvider) {
    super(Process.class, repositoryProvider, designatorService, eventPublisher);
    this.scopeProvider = scopeProvider;
  }

  @Override
  protected void validateRiskDefinition(
      Process process, RiskDefinitionRef riskDefinitionRef, Domain domain) {
    if (!scopeProvider.canUseRiskDefinition(process, domain, riskDefinitionRef)) {
      throw new IllegalArgumentException(
          String.format(
              "Cannot define risk values for risk definition '%s' because the process %s is not within a scope that uses that risk definition",
              riskDefinitionRef.getIdRef(), process.getIdAsString()));
    }
  }
}
