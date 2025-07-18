/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2024  Urs Zeidler
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
package org.veo.service.risk;

import java.time.Instant;
import java.util.Collection;
import java.util.function.Predicate;

import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;
import org.veo.core.entity.RiskAffected;
import org.veo.core.entity.Unit;
import org.veo.core.entity.riskdefinition.RiskDefinition;

public interface ImpactInheritanceCalculator {

  /**
   * Calculates the impact inheritance for an affected Element in a unit for a risk definition.
   * Returns the changed elements.
   */
  Collection<Element> calculateImpactInheritance(
      Unit unit, Domain domain, String riskDefinitionId, RiskAffected<?, ?> affectedElement);

  /**
   * Calculates the impact inheritance for all elements in the unit for a risk definition. Returns
   * the changed elements.
   */
  Collection<? extends Element> updateAllRootNodes(
      Unit unit, Domain domain, String riskDefinitionId);

  default void calculateImpactInheritance(RiskAffected<?, ?> element, Domain domain) {
    Unit owner = element.getOwner();
    domain.getRiskDefinitions().values().stream()
        .filter(hasInheritingLinks())
        .forEach(
            rd -> {
              calculateImpactInheritanceAndUpdateVersions(owner, domain, rd, element);
            });
  }

  default void calculateImpactInheritance(
      RiskAffected<?, ?> element, Domain domain, String linkType) {
    Unit owner = element.getOwner();
    domain.getRiskDefinitions().values().stream()
        .filter(hasInheritingLinks())
        .filter(isInheritingLinkType(linkType))
        .forEach(
            rd -> {
              calculateImpactInheritanceAndUpdateVersions(owner, domain, rd, element);
            });
  }

  default void calculateImpactInheritance(RiskAffected<?, ?> element) {
    Unit owner = element.getOwner();
    Client client = owner.getClient();
    client.getDomains().stream()
        .filter(hasRiskDefinition())
        .forEach(
            domain -> {
              domain.getRiskDefinitions().values().stream()
                  .filter(hasInheritingLinks())
                  .forEach(
                      rd -> {
                        calculateImpactInheritanceAndUpdateVersions(owner, domain, rd, element);
                      });
            });
  }

  private void calculateImpactInheritanceAndUpdateVersions(
      Unit unit, Domain domain, RiskDefinition rd, RiskAffected<?, ?> element) {
    calculateImpactInheritance(unit, domain, rd.getId(), element)
        .forEach(
            updatedElement -> {
              updatedElement.setUpdatedAt(Instant.now());
            });
  }

  default Predicate<? super Domain> hasRiskDefinition() {
    return d -> !d.getRiskDefinitions().isEmpty();
  }

  default Predicate<? super RiskDefinition> isInheritingLinkType(String linkType) {
    return rd ->
        rd.getImpactInheritingLinks().values().stream()
            .flatMap(Collection::stream)
            .anyMatch(l -> l.equals(linkType));
  }

  default Predicate<? super RiskDefinition> hasInheritingLinks() {
    return rd -> !rd.getImpactInheritingLinks().isEmpty();
  }

  default void updateAllRootNodes(Unit unit) {
    unit.getDomains().forEach(domain -> updateAllRootNodes(unit, domain));
  }

  default void updateAllRootNodes(Unit unit, Domain domain) {
    domain.getRiskDefinitions().values().stream()
        .filter(hasInheritingLinks())
        .forEach(rd -> updateAllRootNodes(unit, domain, rd.getId()));
  }
}
