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

import java.util.HashSet;

import org.veo.core.entity.CustomAspect;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;
import org.veo.core.entity.definitions.CustomAspectDefinition;
import org.veo.core.entity.definitions.ElementTypeDefinition;
import org.veo.core.entity.definitions.LinkDefinition;

import lombok.extern.slf4j.Slf4j;

/**
 * Migrates element to the current element type definition. Invalid information (such as obsolete
 * custom aspects) is mercilessly removed from the element.
 */
@Slf4j
public class ElementMigrationService {

  public void migrate(Element element, ElementTypeDefinition definition, Domain domain) {
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
              cleanUpAttributes(element, ca, caDefinition);
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
              cleanUpAttributes(element, link, linkDefinition);
            });
    migrateSubType(domain, definition, element);
  }

  private void migrateSubType(Domain domain, ElementTypeDefinition definition, Element element) {
    element
        .getSubType(domain)
        .ifPresent(
            subType -> {
              var subTypeDefinition = definition.getSubTypes().get(subType);
              if (subTypeDefinition == null) {
                log.debug(
                    "Removing obsolete sub type {} of element {}",
                    subType,
                    element.getIdAsString());
                element.setSubType(domain, null, null);
                return;
              }
              var status = element.getStatus(domain).orElseThrow();
              if (!subTypeDefinition.getStatuses().contains(status)) {
                var fallbackStatus =
                    subTypeDefinition.getStatuses().stream().findFirst().orElseThrow();
                log.debug(
                    "Replacing obsolete status {} of element {} with fallback status {}",
                    status,
                    element.getIdAsString(),
                    fallbackStatus);
                element.setSubType(domain, subType, fallbackStatus);
              }
            });
  }

  private void cleanUpAttributes(
      Element element, CustomAspect caOrLink, CustomAspectDefinition definition) {
    new HashSet<>(caOrLink.getAttributes().keySet())
        .forEach(
            attrType -> {
              if (!definition.getAttributeSchemas().containsKey(attrType)) {
                log.debug(
                    "Removing obsolete attribute {} on {} from element {}.",
                    attrType,
                    caOrLink.getType(),
                    element.getIdAsString());
                caOrLink.getAttributes().remove(attrType);
              }
            });
  }

  private boolean isValidTarget(Element target, Domain domain, LinkDefinition linkDef) {
    return linkDef.getTargetType().equals(target.getModelType())
        && linkDef.getTargetSubType().equals(target.getSubType(domain).orElse(null));
  }
}
