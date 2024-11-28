/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Alexander Koderman
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
package org.veo.persistence.access;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.veo.core.entity.Client;
import org.veo.core.entity.Control;
import org.veo.core.entity.Identifiable;
import org.veo.core.entity.Person;
import org.veo.core.entity.compliance.ControlImplementation;
import org.veo.core.entity.compliance.RequirementImplementation;
import org.veo.core.repository.ControlImplementationQuery;
import org.veo.core.repository.ControlImplementationRepository;
import org.veo.persistence.access.jpa.ControlImplementationDataRepository;
import org.veo.persistence.access.query.ControlImplementationQueryImpl;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ControlImplementationRepositoryImpl implements ControlImplementationRepository {

  private final ControlImplementationDataRepository dataRepo;

  @Override
  public Set<ControlImplementation> findByControls(Set<Control> removedControls) {
    return dataRepo.findByControlIdWithOwner(
        removedControls.stream().map(Identifiable::getIdAsString).collect(Collectors.toSet()));
  }

  @Override
  public ControlImplementationQuery query(Client client, UUID domainId) {
    return new ControlImplementationQueryImpl(dataRepo, client, domainId);
  }

  @Override
  public Set<ControlImplementation> findByRequirement(RequirementImplementation reqImpl) {
    var allIds = dataRepo.findIdsByRequirement(reqImpl.getId());
    return dataRepo.findAllById(allIds).stream()
        .map(ControlImplementation.class::cast)
        .collect(Collectors.toSet());
  }

  @Override
  public Set<ControlImplementation> findByPerson(Person responsible) {
    return dataRepo.findByPerson(responsible);
  }

  @Override
  public Set<ControlImplementation> findByPersons(Set<Person> responsibles) {
    return dataRepo.findByPersons(responsibles);
  }
}
