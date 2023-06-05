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
package org.veo.core.usecase.base;

import org.veo.core.entity.Process;
import org.veo.core.repository.RepositoryProvider;
import org.veo.core.usecase.decision.Decider;
import org.veo.core.usecase.service.EntityStateMapper;

public class UpdateProcessInDomainUseCase extends UpdateElementInDomainUseCase<Process> {
  public UpdateProcessInDomainUseCase(
      RepositoryProvider repositoryProvider, Decider decider, EntityStateMapper entityStateMapper) {
    super(
        repositoryProvider.getElementRepositoryFor(Process.class),
        repositoryProvider,
        decider,
        entityStateMapper);
  }
}
