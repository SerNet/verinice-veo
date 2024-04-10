/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Jonas Jordan
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
package org.veo.core.entity.specification;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.veo.core.entity.CompositeElement;
import org.veo.core.entity.CustomLink;
import org.veo.core.entity.Element;
import org.veo.core.entity.RiskAffected;
import org.veo.core.entity.Scope;
import org.veo.core.entity.compliance.ControlImplementation;
import org.veo.core.entity.compliance.RequirementImplementation;

public class ElementOnlyReferencesItsOwnUnitSpecification implements EntitySpecification<Element> {
  @Override
  public boolean test(Element entity) {
    var referencedElements = getElements(entity);
    return (referencedElements.stream()
        .map(Element::getOwner)
        .distinct()
        .allMatch(u -> u.equals(entity.getOwner())));
  }

  private static List<Element> getElements(Element entity) {
    List<Element> referencedElements =
        entity.getLinks().stream().map(CustomLink::getTarget).collect(Collectors.toList());
    referencedElements.addAll(entity.getScopes());
    if (entity instanceof CompositeElement<?> comp) {
      referencedElements.addAll(comp.getParts());
      referencedElements.addAll(comp.getComposites());
    }
    if (entity instanceof RiskAffected<?, ?> ra) {
      referencedElements.addAll(
          ra.getControlImplementations().stream().map(ControlImplementation::getControl).toList());
      referencedElements.addAll(
          ra.getControlImplementations().stream()
              .map(ControlImplementation::getResponsible)
              .filter(Objects::nonNull)
              .toList());
      referencedElements.addAll(
          ra.getRequirementImplementations().stream()
              .map(RequirementImplementation::getResponsible)
              .filter(Objects::nonNull)
              .toList());
    }
    if (entity instanceof Scope scope) {
      referencedElements.addAll(scope.getMembers());
    }
    return referencedElements;
  }
}
