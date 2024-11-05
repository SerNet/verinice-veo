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

import static java.util.stream.StreamSupport.stream;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.github.zafarkhaja.semver.Version;

import org.veo.core.entity.DomainTemplate;
import org.veo.core.entity.Key;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.repository.DomainTemplateRepository;
import org.veo.persistence.access.jpa.CatalogItemDataRepository;
import org.veo.persistence.access.jpa.DomainTemplateDataRepository;
import org.veo.persistence.access.jpa.ProfileItemDataRepository;
import org.veo.persistence.entity.jpa.DomainTemplateData;
import org.veo.persistence.entity.jpa.ValidationService;

@Repository
public class DomainTemplateRepositoryImpl
    extends AbstractIdentifiableVersionedRepository<DomainTemplate, DomainTemplateData>
    implements DomainTemplateRepository {

  private final DomainTemplateDataRepository dataRepository;
  private final ProfileItemDataRepository profileItemDataRepository;
  private final CatalogItemDataRepository catalogItemDataRepository;

  public DomainTemplateRepositoryImpl(
      DomainTemplateDataRepository dataRepository,
      ValidationService validator,
      ProfileItemDataRepository profileItemDataRepository,
      CatalogItemDataRepository catalogItemDataRepository) {
    super(dataRepository, validator);
    this.dataRepository = dataRepository;
    this.profileItemDataRepository = profileItemDataRepository;
    this.catalogItemDataRepository = catalogItemDataRepository;
  }

  @Override
  public List<DomainTemplate> findAll() {
    return stream(dataRepository.findAll().spliterator(), false)
        .map(DomainTemplate.class::cast)
        .toList();
  }

  @Override
  public List<Key<UUID>> getDomainTemplateIds(String name) {
    return dataRepository.findTemplateIdsByName(name).stream().map(Key::from).toList();
  }

  @Override
  public Optional<Key<UUID>> getLatestDomainTemplateId(String name) {
    return dataRepository.findLatestTemplateIdByName(name).map(Key::from);
  }

  @Override
  public Version getLatestVersion(String templateName) {
    return dataRepository
        .findCurrentTemplateVersion(templateName)
        .map(Version::parse)
        .orElseThrow(
            () ->
                new NotFoundException(
                    "No domain template found for name '%s'".formatted(templateName)));
  }

  @Override
  public Optional<DomainTemplate> findByIdWithProfilesAndRiskDefinitions(Key<UUID> id) {
    return dataRepository
        .findByIdWithProfilesAndRiskDefinitions(id.value())
        .map(DomainTemplate.class::cast);
  }

  @Override
  public DomainTemplate getByIdWithRiskDefinitionsProfilesAndCatalogItems(Key<UUID> id) {
    var dt =
        dataRepository
            .findByIdWithProfilesAndRiskDefinitions(id.value())
            .orElseThrow(() -> new NotFoundException(id, DomainTemplate.class));
    catalogItemDataRepository.findAllByDomainTemplateFetchTailoringReferences(id.value());
    profileItemDataRepository.findAllByDomainTemplateFetchTailoringReferences(id.value());
    return dt;
  }

  @Override
  public boolean templateExists(String name, Version version) {
    return dataRepository.existsByNameAndTemplateVersion(name, version.toString());
  }
}
