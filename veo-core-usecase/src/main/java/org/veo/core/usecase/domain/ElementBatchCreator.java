/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Jochen Kemnade.
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
package org.veo.core.usecase.domain;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.veo.core.entity.AbstractRisk;
import org.veo.core.entity.CompositeElement;
import org.veo.core.entity.CustomLink;
import org.veo.core.entity.Designated;
import org.veo.core.entity.Element;
import org.veo.core.entity.EntityType;
import org.veo.core.entity.RiskAffected;
import org.veo.core.entity.Scope;
import org.veo.core.entity.Unit;
import org.veo.core.entity.event.RiskAffectingElementChangeEvent;
import org.veo.core.repository.GenericElementRepository;
import org.veo.core.service.EventPublisher;
import org.veo.core.usecase.DesignatorService;
import org.veo.core.usecase.decision.Decider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class ElementBatchCreator {
  private final GenericElementRepository genericElementRepository;
  private final EventPublisher eventPublisher;
  private final Decider decider;
  private final DesignatorService designatorService;

  /**
   * Persists a batch of transient elements (and contained risks) in a way that doesn't make JPA
   * throw up. Assigns designators.
   */
  public void create(Collection<Element> elements, Unit unit) {
    Map<Element, Set<CustomLink>> links = new HashMap<>();

    Map<RiskAffected, Set<AbstractRisk>> risks = new HashMap<>();
    // save links after the elements
    elements.stream()
        .filter(e -> !e.getLinks().isEmpty())
        .forEach(
            e -> {
              links.put(e, Set.copyOf(e.getLinks()));
              e.getLinks().clear();
            });
    // save risks after the elements
    elements.stream()
        .filter(RiskAffected.class::isInstance)
        .map(RiskAffected.class::cast)
        .filter(e -> !e.getRisks().isEmpty())
        .forEach(
            e -> {
              risks.put(e, Set.copyOf(e.getRisks()));
              e.getRisks().clear();
            });

    // save scope memberships after the elements
    Map<Scope, Set<Element>> scopeMemberships =
        elements.stream()
            .filter(Scope.class::isInstance)
            .map(Scope.class::cast)
            .collect(
                Collectors.toMap(
                    Function.identity(),
                    comp -> {
                      var members = new HashSet<>(comp.getMembers());
                      comp.getMembers().clear();
                      return members;
                    }));

    // save composite relations after the elements
    Map<CompositeElement<?>, Set<CompositeElement<?>>> compositeRelations =
        elements.stream()
            .filter(CompositeElement.class::isInstance)
            .map(e -> (CompositeElement<?>) e)
            .collect(
                Collectors.toMap(
                    Function.identity(),
                    comp -> {
                      var parts = new HashSet<CompositeElement<?>>(comp.getParts());
                      comp.getParts().clear();
                      return parts;
                    }));

    prepareElements(elements, unit);
    saveElements(elements);

    links.forEach(
        (element, elementLinks) -> {
          elementLinks.forEach(element::applyLink);
        });

    risks.forEach(
        (element, elementRisks) -> {
          elementRisks.forEach(
              r -> {
                if (r.getDesignator() == null) {
                  designatorService.assignDesignator(r, unit.getClient());
                }
                element.addRisk(r);
              });
        });

    scopeMemberships.forEach(Scope::addMembers);

    compositeRelations.forEach(
        (composite, parts) -> {
          composite.addParts((Set) parts);
        });

    elements.forEach(
        element ->
            element
                .getDomains()
                .forEach(d -> element.setDecisionResults(decider.decide(element, d), d)));

    elements.stream()
        .filter(r -> r instanceof RiskAffected)
        .map(r -> (RiskAffected<?, ?>) r)
        .filter(r -> !r.getRisks().isEmpty())
        .forEach(it -> eventPublisher.publish(new RiskAffectingElementChangeEvent(it, this)));
    log.info("{} elements added to unit {}", elements.size(), unit.getIdAsString());
  }

  private void saveElements(Collection<Element> elements) {
    log.debug("Saving {} entities", elements.size());
    // save controls first, so they can be referenced by requirement and control implementations
    genericElementRepository.saveAll(
        elements.stream()
            .sorted(
                Comparator.comparing(
                    it -> !it.getModelType().equals(EntityType.CONTROL.getSingularTerm())))
            .toList());

    log.debug("Done");
  }

  private void prepareElements(Collection<Element> elements, Unit unit) {
    log.debug("Preparing elements {}", elements);

    for (Element element : elements) {
      element.setOwner(unit);
    }
    designatorService.assignDesignators(
        elements.stream()
            .filter(it -> it.getDesignator() == null)
            .map(Designated.class::cast)
            .toList(),
        unit.getClient());
  }
}
