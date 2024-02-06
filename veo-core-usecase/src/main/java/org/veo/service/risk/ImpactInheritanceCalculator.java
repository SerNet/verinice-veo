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

import java.util.Collection;

import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;
import org.veo.core.entity.RiskAffected;
import org.veo.core.entity.Unit;

public interface ImpactInheritanceCalculator {

  /**
   * Calculates the Impact Inheritance for an affected Element in a unit for a risk definition.
   * Returns the changed elements.
   */
  Collection<Element> calculateImpactInheritance(
      Unit unit, Domain domain, String riskDefinitionId, RiskAffected<?, ?> affectedElement);

  default void calculateImpactInheritance(RiskAffected<?, ?> element) {
    Unit owner = element.getOwner();
    Client client = owner.getClient();
    client.getDomains().stream()
        .forEach(
            domain -> {
              domain.getRiskDefinitions().entrySet().stream()
                  .forEach(
                      rd -> {
                        calculateImpactInheritance(owner, domain, rd.getValue().getId(), element);
                      });
            });
  }
}
