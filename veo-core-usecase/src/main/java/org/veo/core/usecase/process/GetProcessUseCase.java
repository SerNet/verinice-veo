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
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.repository.DomainRepository;
import org.veo.core.repository.ProcessRepository;
import org.veo.core.usecase.base.GetElementUseCase;

/** Reinstantiate a persisted process object. */
public class GetProcessUseCase extends GetElementUseCase<Process> {

  private final ProcessRepository processRepository;

  public GetProcessUseCase(ProcessRepository repository, DomainRepository domainRepository) {
    super(domainRepository, repository, Process.class);
    processRepository = repository;
  }

  @Override
  public GetElementUseCase.OutputData<Process> execute(InputData input) {
    var process =
        processRepository
            .findById(input.elementId(), input.embedRisks())
            .orElseThrow(() -> new NotFoundException(input.elementId(), Process.class));
    process.checkSameClient(input.authenticatedClient());
    return new GetElementUseCase.OutputData<>(process, getDomain(process, input).orElse(null));
  }
}
