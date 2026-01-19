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

import static org.veo.core.entity.riskdefinition.RiskDefinitionChange.addedRiskValueCategories;
import static org.veo.core.entity.riskdefinition.RiskDefinitionChange.isPropablilityChanged;
import static org.veo.core.entity.riskdefinition.RiskDefinitionChange.removedImpactCategories;
import static org.veo.core.entity.riskdefinition.RiskDefinitionChange.removedRiskValueCategories;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;
import org.veo.core.entity.RiskAffected;
import org.veo.core.entity.RiskRelated;
import org.veo.core.entity.Scenario;
import org.veo.core.entity.definitions.ElementTypeDefinition;
import org.veo.core.entity.definitions.LinkDefinition;
import org.veo.core.entity.definitions.attribute.AttributeDefinition;
import org.veo.core.entity.risk.PotentialProbability;
import org.veo.core.entity.risk.RiskDefinitionRef;
import org.veo.core.entity.riskdefinition.RiskDefinition;
import org.veo.core.entity.riskdefinition.RiskDefinitionChange;
import org.veo.core.usecase.base.AttributeValidator;

import lombok.extern.slf4j.Slf4j;

/**
 * Migrates element to the current element type definition. Invalid information (such as obsolete
 * custom aspects) is mercilessly removed from the element.
 */
@Slf4j
public class ElementMigrationService {

  public void migrate(Element element, Domain domain) {
    var definition = domain.getElementTypeDefinition(element.getType());
    element
        .getCustomAspects(domain)
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
    element
        .getLinks(domain)
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

  public void migrateRiskRelated(
      RiskRelated riskRelated,
      Domain domain,
      RiskDefinition riskDefinition,
      Set<RiskDefinitionChange> detectedChanges) {
    riskRelated
        .getRiskDefinitions(domain)
        .forEach(
            rdRef ->
                domain
                    .findRiskDefinition(rdRef.getIdRef())
                    .ifPresentOrElse(
                        rd -> {
                          if (riskRelated instanceof RiskAffected<?, ?> ra) {
                            migrateRiskAffected(ra, domain, riskDefinition, detectedChanges);
                          } else if (riskRelated instanceof Scenario sce) {
                            Map<RiskDefinitionRef, PotentialProbability> potentialProbability =
                                sce.getPotentialProbability(domain);
                            if (potentialProbability != null
                                && isPropablilityChanged(detectedChanges)) {
                              potentialProbability.put(
                                  riskDefinition.toRef(), new PotentialProbability(null));
                              sce.setPotentialProbability(domain, potentialProbability);
                            }
                          }
                        },
                        () -> riskRelated.removeRiskDefinition(rdRef, domain)));
  }

  private void migrateRiskAffected(
      RiskAffected<?, ?> ra,
      Domain domain,
      RiskDefinition rd,
      Set<RiskDefinitionChange> detectedChanges) {
    RiskDefinitionRef rdRef = rd.toRef();

    migrateImpacts(ra, domain, rd, detectedChanges);

    ra.getRisks().stream()
        .filter(r -> r.getRiskDefinitions(domain).contains(rdRef))
        .forEach(
            risk -> {
              removedRiskValueCategories(detectedChanges)
                  .forEach(badCat -> risk.removeRiskCategory(rdRef, badCat, domain));
              addedRiskValueCategories(detectedChanges)
                  .forEach(newCat -> risk.addRiskCategory(rdRef, newCat, domain));
            });
  }

  private boolean migrateSubType(Domain domain, ElementTypeDefinition definition, Element element) {
    return element
        .findSubType(domain)
        .map(
            subType -> {
              var subTypeDefinition = definition.getSubTypes().get(subType);
              if (subTypeDefinition == null) {
                log.debug(
                    "Sub type {} of element {} is obsolete, removing element from domain",
                    subType,
                    element.getIdAsString());
                element.removeFromDomains(domain);
                return false;
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
                element.setStatus(fallbackStatus, domain);
              }
              return true;
            })
        .orElse(false);
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
    return linkDef.getTargetType() == target.getType()
        && linkDef.getTargetSubType().equals(target.findSubType(domain).orElse(null));
  }

  private void migrateImpacts(
      RiskAffected<?, ?> ra,
      Domain domain,
      RiskDefinition rd,
      Set<RiskDefinitionChange> detectedChanges) {
    var rdRef = RiskDefinitionRef.from(rd);
    var impactsForDomain = ra.getImpactValues(domain);
    var oldImpacts = impactsForDomain.getOrDefault(rdRef, null);
    if (oldImpacts == null) return;
    var impactCategoriesToUnset = removedImpactCategories(detectedChanges);
    if (impactCategoriesToUnset.isEmpty()) {
      return;
    }
    impactsForDomain.put(rdRef, oldImpacts.withoutCategories(impactCategoriesToUnset));
  }
}
