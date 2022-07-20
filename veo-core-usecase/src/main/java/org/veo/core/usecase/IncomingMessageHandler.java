/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Jonas Jordan
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
package org.veo.core.usecase;

import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;
import org.veo.core.entity.EntityType;
import org.veo.core.repository.RepositoryProvider;
import org.veo.service.CatalogMigrationService;
import org.veo.service.ElementMigrationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Handles parsed incoming messages. */
@RequiredArgsConstructor
@Slf4j
public class IncomingMessageHandler {
  private final RepositoryProvider repositoryProvider;
  private final ElementMigrationService elementMigrationService;
  private final CatalogMigrationService catalogMigrationService;

  public void handleElementTypeDefinitionUpdate(Domain domain, EntityType elementType) {
    repositoryProvider
        .getElementRepositoryFor((Class<? extends Element>) elementType.getType())
        .findByDomain(domain)
        .forEach(element -> elementMigrationService.migrate(element, domain));

    catalogMigrationService.migrate(elementType, domain);
  }
}
