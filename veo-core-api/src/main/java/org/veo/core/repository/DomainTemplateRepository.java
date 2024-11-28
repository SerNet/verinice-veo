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
package org.veo.core.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.github.zafarkhaja.semver.Version;

import org.veo.core.entity.DomainTemplate;

/**
 * A repository for <code>DomainTemplate</code> entities.
 *
 * <p>Implements basic CRUD operations from the superinterface and extends them with more specific
 * methods - i.e. queries based on particular fields.
 */
public interface DomainTemplateRepository extends Repository<DomainTemplate> {
  List<DomainTemplate> findAll();

  List<UUID> getDomainTemplateIds(String name);

  Optional<UUID> getLatestDomainTemplateId(String name);

  Version getLatestVersion(String templateName);

  Optional<DomainTemplate> findByIdWithProfilesAndRiskDefinitions(UUID id);

  DomainTemplate getByIdWithRiskDefinitionsProfilesAndCatalogItems(UUID id);

  boolean templateExists(String name, Version version);

  Optional<DomainTemplate> findLatestByMajor(String name, long major);
}
