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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.veo.core.entity.AbstractRisk;
import org.veo.core.entity.CompositeElement;
import org.veo.core.entity.CustomLink;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;
import org.veo.core.entity.RiskAffected;
import org.veo.core.entity.Scope;
import org.veo.core.entity.Unit;
import org.veo.core.entity.event.RiskAffectingElementChangeEvent;
import org.veo.core.entity.profile.ProfileRef;
import org.veo.core.repository.ElementRepository;
import org.veo.core.repository.RepositoryProvider;
import org.veo.core.repository.UnitRepository;
import org.veo.core.service.DomainTemplateService;
import org.veo.core.service.EventPublisher;
import org.veo.core.usecase.decision.Decider;
import org.veo.service.ElementMigrationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Applies a profile to a unit by instantiating all profile elements and risks inside the unit. */
@RequiredArgsConstructor
@Slf4j
public class ProfileApplier {
  private static final String DESIGNATOR_PREFIX = "DMO-";

  private final DomainTemplateService domainTemplateService;
  private final UnitRepository unitRepository;
  private final RepositoryProvider repositoryProvider;
  private final EventPublisher eventPublisher;
  private final Decider decider;
  private final ElementMigrationService elementMigrationService;

  public void applyProfile(Domain domain, ProfileRef profile, Unit unit) {
    var profileElements = domainTemplateService.getProfileElements(domain, profile);
    unit.addToDomains(domain);
    unitRepository.save(unit);
    Map<Class<Element>, List<Element>> elementsGroupedByType = groupByType(profileElements);
    Map<Element, Set<CustomLink>> links = new HashMap<>();

    Map<RiskAffected, Set<AbstractRisk>> risks = new HashMap<>();
    // save links after the elements
    profileElements.stream()
        .filter(e -> !e.getLinks().isEmpty())
        .forEach(
            e -> {
              links.put(e, Set.copyOf(e.getLinks()));
              e.getLinks().clear();
            });
    // save risks after the elements
    profileElements.stream()
        .filter(RiskAffected.class::isInstance)
        .map(RiskAffected.class::cast)
        .filter(e -> !e.getRisks().isEmpty())
        .forEach(
            e -> {
              risks.put(e, Set.copyOf(e.getRisks()));
              e.getRisks().clear();
            });

    AtomicInteger counter = new AtomicInteger(0);
    elementsGroupedByType.entrySet().stream()
        // sort entries by model type to get predictable designators
        .sorted(Comparator.comparing(entry -> entry.getKey().getSimpleName()))
        .flatMap(entry -> entry.getValue().stream())
        .forEach(e -> prepareElement(e, unit, counter));
    elementsGroupedByType.forEach(this::saveElements);

    Set<Element> elementsToSave = new HashSet<>(profileElements.size());
    links.forEach(
        (element, elementLinks) -> {
          elementLinks.forEach(element::addToLinks);
          elementsToSave.add(element);
        });

    risks.forEach(
        (element, elementRisks) -> {
          elementRisks.forEach(
              r -> {
                r.setDesignator(DESIGNATOR_PREFIX + counter.incrementAndGet());
                element.addRisk(r);
              });
          elementsToSave.add(element);
        });

    profileElements.forEach(
        element -> {
          var results = decider.decide(element, domain);
          if (!results.isEmpty()) {
            element.setDecisionResults(results, domain);
            elementsToSave.add(element);
          }
        });
    groupByType(elementsToSave).forEach(this::saveElements);
    elementsToSave.forEach(
        it -> eventPublisher.publish(new RiskAffectingElementChangeEvent(it, this)));
    log.info("{} profile elements added to unit {}", profileElements.size(), unit.getIdAsString());
  }

  @SuppressWarnings("unchecked")
  private Map<Class<Element>, List<Element>> groupByType(Collection<Element> elements) {
    return elements.stream()
        .collect(Collectors.groupingBy(e1 -> (Class<Element>) e1.getModelInterface()));
  }

  private <T extends Element> void saveElements(Class<T> entityType, List<T> elementsWithType) {
    ElementRepository<T> elementRepository = repositoryProvider.getElementRepositoryFor(entityType);
    log.debug("Saving {} entities with type {}", elementsWithType.size(), entityType);
    elementRepository.saveAll(Set.copyOf(elementsWithType));
    log.debug("Done");
  }

  private void prepareElement(Element element, Unit unit, AtomicInteger counter) {
    log.debug("Preparing element {}:{}", element.getId(), element);
    element.setDesignator(DESIGNATOR_PREFIX + counter.incrementAndGet());
    element.setOwner(unit);
    // TODO VEO-1547 element migration will become obsolete once the profiles they come from get
    // migrated in the domain.
    element.getDomains().forEach(d -> elementMigrationService.migrate(element, d));

    if (element instanceof CompositeElement<?> ce) {
      ce.getParts().forEach(e -> prepareElement(e, unit, counter));
    } else if (element instanceof Scope scope) {
      Set<Element> members = scope.getMembers();
      members.forEach(m -> prepareElement(m, unit, counter));
    }
  }
}
