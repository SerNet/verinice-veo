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
import java.util.HashSet;
import java.util.Map;

import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;
import org.veo.core.entity.definitions.ElementTypeDefinition;
import org.veo.core.entity.definitions.LinkDefinition;
import org.veo.core.entity.definitions.attribute.AttributeDefinition;
import org.veo.core.usecase.base.AttributeValidator;

import lombok.extern.slf4j.Slf4j;

/**
 * Migrates element to the current element type definition. Invalid information (such as obsolete
 * custom aspects) is mercilessly removed from the element.
 */
@Slf4j
public class ElementMigrationService {

  public void migrate(Element element, Domain domain) {
    var definition = domain.getElementTypeDefinition(element.getModelType());
    new HashSet<>(element.getCustomAspects())
        .forEach(
            ca -> {
              var caDefinition = definition.getCustomAspects().get(ca.getType());
              if (caDefinition == null) {
                log.debug(
                    "Removing obsolete custom aspect {} from element {}.",
                    ca.getType(),
                    element.getIdAsString());
                element.getCustomAspects().remove(ca);
                return;
              }
              migrateAttributes(ca.getAttributes(), caDefinition.getAttributeDefinitions());
            });
    new HashSet<>(element.getLinks())
        .forEach(
            link -> {
              var linkDefinition = definition.getLinks().get(link.getType());
              if (linkDefinition == null) {
                log.debug(
                    "Removing obsolete link {} from element {}.",
                    link.getType(),
                    element.getIdAsString());
                element.getLinks().remove(link);
                return;
              }
              if (!isValidTarget(link.getTarget(), domain, linkDefinition)) {
                log.debug(
                    "Removing link {} from element {} due to invalid target.",
                    link.getType(),
                    element.getIdAsString());
                element.getLinks().remove(link);
                return;
              }
              migrateAttributes(link.getAttributes(), linkDefinition.getAttributeDefinitions());
            });
    migrateSubType(domain, definition, element);
  }

  private void migrateSubType(Domain domain, ElementTypeDefinition definition, Element element) {
    element
        .findSubType(domain)
        .ifPresent(
            subType -> {
              var subTypeDefinition = definition.getSubTypes().get(subType);
              if (subTypeDefinition == null) {
                var catalogItem = element.getContainingCatalogItem();
                if (catalogItem != null) {
                  log.debug(
                      "Sub type {} of catalog item element {} is obsolete, removing catalog item",
                      subType,
                      element.getIdAsString());
                  catalogItem.getCatalog().getCatalogItems().remove(catalogItem);
                } else {
                  log.debug(
                      "Sub type {} of element {} is obsolete, removing element from domain",
                      subType,
                      element.getIdAsString());
                  element.removeFromDomains(domain);
                }
                return;
              }
              var status = element.getStatus(domain);
              if (!subTypeDefinition.getStatuses().contains(status)) {
                var fallbackStatus =
                    subTypeDefinition.getStatuses().stream().findFirst().orElseThrow();
                log.debug(
                    "Replacing obsolete status {} of element {} with fallback status {}",
                    status,
                    element.getIdAsString(),
                    fallbackStatus);
                element.associateWithDomain(domain, subType, fallbackStatus);
              }
            });
  }

  /** Removes all invalid attributes from given custom aspect / link attributes. */
  public void migrateAttributes(
      Map<String, Object> attributes, Map<String, AttributeDefinition> definitions) {
    new HashMap<>(attributes)
        .forEach(
            (attrKey, attrValue) -> {
              try {
                AttributeValidator.validate(attrKey, attrValue, definitions);
              } catch (IllegalArgumentException illEx) {
                log.debug("Attribute validation failed", illEx);
                log.debug("Removing invalidate attribute {}", attrKey);
                attributes.remove(attrKey);
              }
            });
  }

  private boolean isValidTarget(Element target, Domain domain, LinkDefinition linkDef) {
    return linkDef.getTargetType().equals(target.getModelType())
        && linkDef.getTargetSubType().equals(target.findSubType(domain).orElse(null));
  }
}
