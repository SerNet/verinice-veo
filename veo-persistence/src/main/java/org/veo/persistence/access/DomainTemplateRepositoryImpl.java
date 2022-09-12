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

import org.springframework.stereotype.Repository;

import com.github.zafarkhaja.semver.Version;

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

  @Override
  public List<Key<UUID>> getDomainTemplateIds(String name) {
    return dataRepository.findTemplateIdsByName(name).stream().map(Key::uuidFrom).toList();
  }

  @Override
  public Optional<Key<UUID>> getLatestDomainTemplateId(String name) {
    return dataRepository.findLatestTemplateIdByName(name).map(Key::uuidFrom);
  }

  @Override
  public Optional<Version> findCurrentTemplateVersion(String templateName) {
    return dataRepository
        .findCurrentTemplateVersion(templateName)
        .map(
            version -> {
              try {
                return Version.valueOf(version);
              }
              // TODO-1072 This will no longer happen once we've abolished non-sem-vers from the DB.
              catch (Exception ex) {
                return null;
              }
            });
  }

  @Override
  public Optional<DomainTemplate> findByIdWithProfilesAndRiskDefinitions(Key<UUID> id) {
    return dataRepository
        .findByIdWithProfilesAndRiskDefinitions(id.uuidValue())
        .map(DomainTemplate.class::cast);
  }
}
