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
package org.veo.core.usecase.risk;

import java.util.Map;
import java.util.Set;

import org.veo.core.entity.CompositeElement;
import org.veo.core.entity.Control;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Process;
import org.veo.core.entity.Scope;
import org.veo.core.entity.risk.RiskDefinitionRef;
import org.veo.core.usecase.base.ScopeProvider;

import lombok.RequiredArgsConstructor;

/** Validates risk-related values on elements. */
@RequiredArgsConstructor
public class RiskValueValidator {
  private final ScopeProvider scopeProvider;

  /**
   * Validates the risk definitions used by a process. An element can only use a risk definition if
   * it is a member of a scope using that risk definition.
   */
  public void validate(Process element) {
    validate(element, Set.of());
  }

  /**
   * Validates the risk definitions used by a process. An element must only use a risk definition if
   * it is a member of a scope that uses that risk definition.
   *
   * @param uncommittedScopes Transient scope memberships that have not been persisted yet
   */
  public void validate(Process newElement, Set<Scope> uncommittedScopes) {
    newElement
        .getDomains()
        .forEach(
            domain -> {
              newElement
                  .getImpactValues(domain)
                  .ifPresent(
                      impactMap ->
                          validateRiskValues(newElement, domain, impactMap, uncommittedScopes));
            });
  }

  /**
   * Validates the risk definitions used by a control. An element must only use a risk definition if
   * it is a member of a scope that uses that risk definition.
   */
  public void validate(Control element) {
    validate(element, Set.of());
  }

  /**
   * Validates the risk definitions used by a control. An element must only use a risk definition if
   * it is a member of a scope that uses that risk definition.
   *
   * @param uncommittedScopes Transient scope memberships that have not been persisted yet
   */
  public void validate(Control newElement, Set<Scope> uncommittedScopes) {
    newElement
        .getDomains()
        .forEach(
            domain -> {
              newElement
                  .getRiskValues(domain)
                  .ifPresent(
                      impactMap ->
                          validateRiskValues(newElement, domain, impactMap, uncommittedScopes));
            });
  }

  private <T extends CompositeElement<T>> void validateRiskValues(
      T element,
      Domain domain,
      Map<RiskDefinitionRef, ?> riskValueMap,
      Set<Scope> uncommittedScopes) {
    riskValueMap
        .keySet()
        .forEach(
            riskDefinitionRef -> {
              if (!scopeProvider.canUseRiskDefinition(
                  element, domain, riskDefinitionRef, uncommittedScopes)) {
                throw new IllegalArgumentException(
                    String.format(
                        "Cannot use risk definition '%s' because the element is not a member of a scope with that risk definition",
                        riskDefinitionRef.getIdRef()));
              }
            });
  }
}
