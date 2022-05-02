/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Urs Zeidler.
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
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.springframework.stereotype.Repository;

import org.veo.core.entity.DomainTemplate;
import org.veo.core.entity.Key;
import org.veo.core.repository.DomainTemplateRepository;
import org.veo.persistence.access.jpa.DomainTemplateDataRepository;
import org.veo.persistence.entity.jpa.DomainTemplateData;
import org.veo.persistence.entity.jpa.ValidationService;

@Repository
public class DomainTemplateRepositoryImpl
    extends AbstractIdentifiableVersionedRepository<DomainTemplate, DomainTemplateData>
    implements DomainTemplateRepository {

  private final DomainTemplateDataRepository dataRepository;

  public DomainTemplateRepositoryImpl(
      DomainTemplateDataRepository dataRepository, ValidationService validator) {
    super(dataRepository, validator);
    this.dataRepository = dataRepository;
  }

  public List<DomainTemplate> getAll() {
    return StreamSupport.stream(dataRepository.findAll().spliterator(), false)
        .map(e -> (DomainTemplate) e)
        .collect(Collectors.toList());
  }

  @Override
  public Optional<Key<UUID>> getLatestDomainTemplateId(String name) {
    return dataRepository.findLatestTemplateIdByName(name).map(Key::uuidFrom);
  }
}
