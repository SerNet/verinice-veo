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

import org.veo.core.entity.Incident;
import org.veo.core.repository.RepositoryProvider;
import org.veo.core.usecase.decision.Decider;
import org.veo.core.usecase.service.EntityStateMapper;
import org.veo.core.usecase.service.RefResolverFactory;

public class UpdateIncidentInDomainUseCase extends UpdateElementInDomainUseCase<Incident> {

  public UpdateIncidentInDomainUseCase(
      RepositoryProvider repositoryProvider,
      Decider decider,
      EntityStateMapper entityStateMapper,
      RefResolverFactory refResolverFactory) {
    super(
        repositoryProvider.getElementRepositoryFor(Incident.class),
        refResolverFactory,
        decider,
        entityStateMapper);
  }
}
