/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2019  Urs Zeidler.
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import org.veo.core.entity.Client;
import org.veo.core.entity.Key;
import org.veo.core.entity.Unit;
import org.veo.core.repository.UnitRepository;
import org.veo.persistence.access.jpa.UnitDataRepository;
import org.veo.persistence.entity.jpa.UnitData;
import org.veo.persistence.entity.jpa.ValidationService;

@Repository
public class UnitRepositoryImpl extends AbstractIdentifiableVersionedRepository<Unit, UnitData>
    implements UnitRepository {

  private final UnitDataRepository dataRepository;

  public UnitRepositoryImpl(UnitDataRepository dataRepository, ValidationService validation) {
    super(dataRepository, validation);
    this.dataRepository = dataRepository;
  }

  @Override
  public List<Unit> findByClient(Client client) {
    return dataRepository.findByClientId(client.getIdAsUUID()).stream()
        .map(Unit.class::cast)
        .toList();
  }

  @Override
  public List<Unit> findByParent(Unit parent) {
    return dataRepository.findByParentId(parent.getIdAsUUID()).stream()
        .map(Unit.class::cast)
        .toList();
  }

  @Override
  public Optional<Unit> findByIdFetchClient(Key<UUID> id) {
    return dataRepository.findWithClientByDbId(id.value()).map(Unit.class::cast);
  }

  @Override
  public List<Unit> findByDomain(Key<UUID> domainId) {
    return dataRepository.findByDomainsId(domainId.value()).stream().map(Unit.class::cast).toList();
  }
}
