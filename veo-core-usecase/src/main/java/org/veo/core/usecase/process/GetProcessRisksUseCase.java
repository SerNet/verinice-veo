/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2019  Alexander Koderman.
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

import org.veo.core.entity.Process;
import org.veo.core.entity.ProcessRisk;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.repository.ProcessRepository;
import org.veo.core.repository.RepositoryProvider;
import org.veo.core.usecase.risk.GetRisksUseCase;

public class GetProcessRisksUseCase extends GetRisksUseCase<Process, ProcessRisk> {

  ProcessRepository processRepository;

  public GetProcessRisksUseCase(
      RepositoryProvider repositoryProvider, ProcessRepository processRepository) {
    super(repositoryProvider, Process.class);
    this.processRepository = processRepository;
  }

  @Override
  public OutputData<ProcessRisk> execute(InputData input) {
    var process =
        processRepository
            .findByIdWithRiskValues(input.riskAffectedRef())
            .orElseThrow(
                () ->
                    new NotFoundException(
                        "Could not find risks for process %s",
                        input.riskAffectedRef().uuidValue()));

    process.checkSameClient(input.authenticatedClient());
    return new OutputData<>(process.getRisks());
  }
}
