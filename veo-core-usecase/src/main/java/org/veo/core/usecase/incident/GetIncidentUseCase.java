/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Jonas Jordan.
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
package org.veo.core.usecase.incident;

import org.veo.core.entity.Incident;
import org.veo.core.repository.DomainRepository;
import org.veo.core.repository.IncidentRepository;
import org.veo.core.usecase.base.GetElementUseCase;

/** Reinstantiate a persisted incident object. */
public class GetIncidentUseCase extends GetElementUseCase<Incident> {

  public GetIncidentUseCase(IncidentRepository repository, DomainRepository domainRepository) {
    super(domainRepository, repository, Incident.class);
  }
}
