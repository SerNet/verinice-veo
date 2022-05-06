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
package org.veo.core.usecase.base;

import java.util.HashSet;
import java.util.Set;

import org.veo.core.entity.CompositeElement;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;
import org.veo.core.entity.Scope;
import org.veo.core.entity.risk.RiskDefinitionRef;
import org.veo.core.repository.CompositeElementRepository;
import org.veo.core.repository.RepositoryProvider;
import org.veo.core.repository.ScopeRepository;

import lombok.RequiredArgsConstructor;

/** Provides information about the {@link Scope} memberships of {@link Element}s. */
@RequiredArgsConstructor
public class ScopeProvider {
  private final RepositoryProvider repositoryProvider;
  private final ScopeRepository scopeRepository;

  /**
   * Determines whether given process may use given risk definition due to its persisted (direct or
   * indirect) scope memberships.
   */
  public <T extends CompositeElement<T>> boolean canUseRiskDefinition(
      T element, Domain domain, RiskDefinitionRef riskDefinitionRef) {
    return canUseRiskDefinition(element, domain, riskDefinitionRef, Set.of());
  }

  /**
   * Determines whether given process may use given risk definition due to its transient or
   * persisted (direct or indirect) scope memberships.
   *
   * @param uncommittedScopes transient scope memberships that are yet to be persisted
   */
  public <T extends CompositeElement<T>> boolean canUseRiskDefinition(
      T element, Domain domain, RiskDefinitionRef riskDefinitionRef, Set<Scope> uncommittedScopes) {
    var repo =
        (CompositeElementRepository<T>)
            repositoryProvider.getRepositoryFor(element.getModelInterface());
    return canUseRiskDefinition(element, domain, riskDefinitionRef, repo, uncommittedScopes);
  }

  private <TElement extends CompositeElement<TElement>> boolean canUseRiskDefinition(
      TElement element,
      Domain domain,
      RiskDefinitionRef riskDef,
      CompositeElementRepository<TElement> repo,
      Set<Scope> uncommittedScopes) {
    // Check if any of the transient scope memberships allow the element to use the risk definition
    if (uncommittedScopes.stream()
        .map(s -> s.getRiskDefinition(domain).orElse(null))
        .anyMatch(riskDef::equals)) {
      return true;
    }
    // Traverse element's composite hierarchy upwards to find its superordinate
    // composite elements
    var encounteredElements = new HashSet<Element>();
    var elementsOnCurrentLevel = Set.of(element);
    do {
      if (scopeRepository.canUseRiskDefinition(elementsOnCurrentLevel, riskDef, domain)) {
        return true;
      }
      encounteredElements.addAll(elementsOnCurrentLevel);
      elementsOnCurrentLevel = repo.findCompositesByParts(elementsOnCurrentLevel);
      // Ignore elements that have already been encountered (to handle circular
      // structures).
      elementsOnCurrentLevel.removeIf(encounteredElements::contains);
    } while (!elementsOnCurrentLevel.isEmpty());
    return false;
  }
}
