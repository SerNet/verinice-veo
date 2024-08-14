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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;
import org.veo.core.entity.RiskAffected;
import org.veo.core.entity.definitions.ElementTypeDefinition;
import org.veo.core.entity.definitions.LinkDefinition;
import org.veo.core.entity.definitions.attribute.AttributeDefinition;
import org.veo.core.entity.risk.CategoryRef;
import org.veo.core.entity.risk.ImpactValues;
import org.veo.core.entity.risk.RiskDefinitionRef;
import org.veo.core.entity.riskdefinition.RiskDefinition;
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
    boolean stillInDomain = migrateSubType(domain, definition, element);

    if (stillInDomain && element instanceof RiskAffected<?, ?> ra) {
      migrateRiskAffected(ra, domain);
    }
    // TODO: remove scenario probability values for deleted risk definitions

  }

  public void migrateRiskAffected(RiskAffected<?, ?> ra, Domain domain) {
    Map<RiskDefinitionRef, ImpactValues> impactValues = ra.getImpactValues(domain);
    impactValues
        .entrySet()
        .forEach(
            e -> {
              domain
                  .getRiskDefinition(e.getKey().getIdRef())
                  .ifPresentOrElse(
                      rd -> migrateRiskAffected(ra, domain, rd),
                      () -> removeRiskDefinition(ra, domain, e.getKey()));
            });
  }

  private void removeRiskDefinition(RiskAffected<?, ?> ra, Domain domain, RiskDefinitionRef rd) {
    ra.removeRiskDefinition(rd, domain);
    ra.getRisks()
        .forEach(
            risk -> {
              risk.removeRiskDefinition(rd, domain);
            });
  }

  public void migrateRiskAffected(RiskAffected<?, ?> ra, Domain domain, RiskDefinition rd) {
    RiskDefinitionRef rdRef = RiskDefinitionRef.from(rd);
    migrateImpacts(ra, domain, rd);

    ra.getRisks()
        .forEach(
            risk -> {
              rd.getCategories()
                  .forEach(
                      cat -> {
                        if (!cat.isRiskValuesSupported()) {
                          risk.removeRiskCategory(
                              RiskDefinitionRef.from(rd), CategoryRef.from(cat), domain);
                        } else if (cat.isRiskValuesSupported()
                            && risk.getRiskDefinitions(domain).contains(rdRef)) {
                          risk.addRiskCategory(
                              RiskDefinitionRef.from(rd), CategoryRef.from(cat), domain);
                        }
                      });

              if (risk.getRiskDefinitions(domain).contains(rdRef)) {
                List<CategoryRef> availableCats =
                    rd.getCategories().stream().map(CategoryRef::from).toList();
                risk.getImpactProvider(rdRef, domain).getAvailableCategories().stream()
                    .filter(c -> !availableCats.contains(c))
                    .forEach(cat -> risk.removeRiskCategory(rdRef, cat, domain));
              }
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
    return linkDef.getTargetType().equals(target.getModelType())
        && linkDef.getTargetSubType().equals(target.findSubType(domain).orElse(null));
  }

  private void migrateImpacts(RiskAffected<?, ?> ra, Domain domain, RiskDefinition rd) {
    var rdRef = RiskDefinitionRef.from(rd);
    var impactsForDomain = ra.getImpactValues(domain);
    var oldImpacts = impactsForDomain.getOrDefault(rdRef, null);
    if (oldImpacts == null) return;
    var validCats = rd.getCategories().stream().map(CategoryRef::from).toList();
    impactsForDomain.put(
        rdRef,
        new ImpactValues(
            removeInvalidKeys(oldImpacts.potentialImpacts(), validCats),
            removeInvalidKeys(oldImpacts.potentialImpactsCalculated(), validCats),
            removeInvalidKeys(oldImpacts.potentialImpactReasons(), validCats),
            removeInvalidKeys(oldImpacts.potentialImpactExplanations(), validCats)));
  }

  private <TKey, TValue> Map<TKey, TValue> removeInvalidKeys(
      Map<TKey, TValue> map, List<TKey> validKeys) {
    return map.entrySet().stream()
        .filter(e -> validKeys.contains(e.getKey()))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }
}
