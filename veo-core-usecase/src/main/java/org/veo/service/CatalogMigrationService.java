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
package org.veo.service;

import java.util.HashMap;
import java.util.Set;

import org.veo.core.entity.CatalogItem;
import org.veo.core.entity.Domain;
import org.veo.core.entity.EntityType;
import org.veo.core.entity.LinkTailoringReference;
import org.veo.core.repository.CatalogItemRepository;
import org.veo.core.usecase.base.CatalogItemValidator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Migrates domain-specific information on catalog items when an element type definition on a domain
 * is updated. Removes invalid custom aspects, link references, attributes and subtypes.
 */
@RequiredArgsConstructor
@Slf4j
public class CatalogMigrationService {
  private final ElementMigrationService elementMigrationService;
  private final CatalogItemRepository catalogItemRepository;

  public void migrate(EntityType type, Domain domain) {
    var items = catalogItemRepository.findAllByDomain(domain);

    // Migrate elements
    items.stream()
        .filter(e -> e.getElementType().equals(type.getSingularTerm()))
        .forEach(e -> migrate(e, domain));

    // Migrate link tailoring references
    items.stream()
        .flatMap(ci -> Set.copyOf(ci.getTailoringReferences()).stream())
        .filter(LinkTailoringReference.class::isInstance)
        .map(LinkTailoringReference.class::cast)
        .filter(ltr -> ltr.getLinkSourceItem().getElementType().equals(type.getSingularTerm()))
        .forEach(
            linkTailoringReference ->
                domain
                    .getElementTypeDefinition(type.getSingularTerm())
                    .findLink(linkTailoringReference.getLinkType())
                    .ifPresentOrElse(
                        linkDef -> {
                          elementMigrationService.migrateAttributes(
                              linkTailoringReference.getAttributes(),
                              linkDef.getAttributeDefinitions());
                          try {
                            CatalogItemValidator.validate(linkTailoringReference, domain);
                          } catch (IllegalArgumentException illEx) {
                            log.info("Tailoring reference validation failed", illEx);
                            log.info(
                                "Deleting invalid tailoring reference {}",
                                linkTailoringReference.getIdAsString());
                            linkTailoringReference.remove();
                          }
                        },
                        () -> {
                          log.info(
                              "Link type {} for tailoring reference is invalid, deleting invalid tailoring reference {}",
                              linkTailoringReference.getLinkType(),
                              linkTailoringReference.getIdAsString());
                          linkTailoringReference.remove();
                        }));
  }

  private void migrate(CatalogItem item, Domain domain) {
    var definition = domain.getElementTypeDefinition(item.getElementType());
    new HashMap<>(item.getCustomAspects())
        .entrySet()
        .forEach(
            entry -> {
              var caDefinition = definition.getCustomAspects().get(entry.getKey());
              if (caDefinition == null) {
                log.debug(
                    "Removing obsolete custom aspect {} from element {}.",
                    entry.getKey(),
                    item.getIdAsString());
                item.getCustomAspects().remove(entry.getKey());
                return;
              }
              elementMigrationService.migrateAttributes(
                  entry.getValue(), caDefinition.getAttributeDefinitions());
            });
  }
}
