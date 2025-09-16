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
import java.util.stream.Collectors;

import org.veo.core.entity.Client;
import org.veo.core.entity.Control;
import org.veo.core.entity.Identifiable;
import org.veo.core.entity.compliance.ReqImplRef;
import org.veo.core.entity.compliance.RequirementImplementation;
import org.veo.core.repository.RequirementImplementationQuery;
import org.veo.core.repository.RequirementImplementationRepository;
import org.veo.persistence.access.jpa.RequirementImplementationDataRepository;
import org.veo.persistence.access.query.RequirementImplementationQueryImpl;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RequirementImplementationRepositoryImpl
    implements RequirementImplementationRepository {

  private final RequirementImplementationDataRepository dataRepo;

  @Override
  public RequirementImplementationQuery query(Client client) {
    return new RequirementImplementationQueryImpl(dataRepo, client);
  }

  @Override
  public Set<RequirementImplementation> findByControls(Set<Control> removedControls) {
    return dataRepo
        .findAllByControlIds(
            removedControls.stream().map(Identifiable::getIdAsString).collect(Collectors.toSet()))
        .stream()
        .map(RequirementImplementation.class::cast)
        .collect(Collectors.toSet());
  }

  @Override
  public Set<RequirementImplementation> findAllByRef(Set<ReqImplRef> requirementImplementations) {
    return dataRepo
        .findAllByUUID(
            requirementImplementations.stream()
                .map(ReqImplRef::getUUID)
                .collect(Collectors.toSet()))
        .stream()
        .map(RequirementImplementation.class::cast)
        .collect(Collectors.toSet());
  }
}
