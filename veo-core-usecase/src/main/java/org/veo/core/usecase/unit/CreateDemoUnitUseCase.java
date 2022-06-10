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
package org.veo.core.usecase.unit;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.transaction.Transactional;
import javax.validation.Valid;

import org.veo.core.entity.AbstractRisk;
import org.veo.core.entity.Client;
import org.veo.core.entity.CompositeElement;
import org.veo.core.entity.CustomLink;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;
import org.veo.core.entity.Key;
import org.veo.core.entity.RiskAffected;
import org.veo.core.entity.Scope;
import org.veo.core.entity.Unit;
import org.veo.core.entity.event.RiskAffectingElementChangeEvent;
import org.veo.core.entity.transform.EntityFactory;
import org.veo.core.repository.ClientRepository;
import org.veo.core.repository.ElementRepository;
import org.veo.core.repository.RepositoryProvider;
import org.veo.core.repository.UnitRepository;
import org.veo.core.service.DomainTemplateService;
import org.veo.core.service.EventPublisher;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.decision.Decider;

import lombok.Value;
import lombok.extern.slf4j.Slf4j;

/** Create a new demo unit for a client. */
@Slf4j
public class CreateDemoUnitUseCase
    implements TransactionalUseCase<
        CreateDemoUnitUseCase.InputData, CreateDemoUnitUseCase.OutputData> {

  private static final String DEMO_UNIT_DESIGNATOR_PREFIX = "DMO-";
  public static final String DEMO_UNIT_NAME = "Demo";
  private final ClientRepository clientRepository;
  private final UnitRepository unitRepository;
  private final EntityFactory entityFactory;
  private final DomainTemplateService domainTemplateService;
  private final RepositoryProvider repositoryProvider;
  private final EventPublisher eventPublisher;
  private final Decider decider;

  public CreateDemoUnitUseCase(
      ClientRepository clientRepository,
      UnitRepository unitRepository,
      EntityFactory entityFactory,
      DomainTemplateService domainTemplateService,
      RepositoryProvider repositoryProvider,
      EventPublisher eventPublisher,
      Decider decider) {
    this.clientRepository = clientRepository;
    this.unitRepository = unitRepository;
    this.entityFactory = entityFactory;
    this.domainTemplateService = domainTemplateService;
    this.repositoryProvider = repositoryProvider;
    this.eventPublisher = eventPublisher;
    this.decider = decider;
  }

  @Override
  @Transactional
  public OutputData execute(InputData input) {

    Client client = clientRepository.findById(input.getClientId()).orElseThrow();
    return new OutputData(createDemoUnitForClient(client));
  }

  private Unit createDemoUnitForClient(Client savedClient) {
    Unit demoUnit = entityFactory.createUnit(DEMO_UNIT_NAME, null);
    demoUnit.setClient(savedClient);
    Collection<Element> demoUnitElements =
        domainTemplateService.getElementsForDemoUnit(savedClient);
    Set<Domain> domainsFromElements =
        demoUnitElements.stream()
            .flatMap(element -> element.getDomains().stream())
            .distinct()
            .collect(Collectors.toSet());
    demoUnit.addToDomains(domainsFromElements);
    unitRepository.save(demoUnit);
    Map<Class<Element>, List<Element>> elementsGroupedByType = groupByType(demoUnitElements);
    Map<Element, Set<CustomLink>> links = new HashMap<>();

    Map<RiskAffected, Set<AbstractRisk>> risks = new HashMap<>();
    // save links after the elements
    demoUnitElements.stream()
        .filter(e -> !e.getLinks().isEmpty())
        .forEach(
            e -> {
              links.put(e, Set.copyOf(e.getLinks()));
              e.getLinks().clear();
            });
    // save risks after the elements
    demoUnitElements.stream()
        .filter(e -> e instanceof RiskAffected)
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
        .forEach(e -> prepareAndSaveElements(e.getKey(), e.getValue(), demoUnit, counter));

    Set<Element> elementsToSave = new HashSet<>(demoUnitElements.size());

    links.entrySet().stream()
        .forEach(
            e -> {
              e.getValue().stream()
                  .forEach(
                      l -> {
                        e.getKey().addToLinks(l);
                        elementsToSave.add(e.getKey());
                      });
            });
    risks.entrySet().stream()
        .forEach(
            e ->
                e.getValue().stream()
                    .forEach(
                        r -> {
                          r.setDesignator(DEMO_UNIT_DESIGNATOR_PREFIX + counter.incrementAndGet());
                          elementsToSave.add(e.getKey());
                          e.getKey().addRisk(r);
                        }));

    demoUnitElements.stream()
        .forEach(
            element ->
                domainsFromElements.forEach(
                    domain -> {
                      var results = decider.decide(element, domain);
                      if (!results.isEmpty()) {
                        element.setDecisionResults(results, domain);
                        elementsToSave.add(element);
                      }
                    }));
    groupByType(elementsToSave).entrySet().stream()
        .forEach(e -> saveElements(e.getKey(), e.getValue()));
    elementsToSave.forEach(
        it -> eventPublisher.publish(new RiskAffectingElementChangeEvent(it, this)));
    log.info("Demo unit with {} elements created", demoUnitElements.size());
    return demoUnit;
  }

  @SuppressWarnings("unchecked")
  private Map<Class<Element>, List<Element>> groupByType(Collection<Element> elements) {
    return elements.stream()
        .collect(Collectors.groupingBy(e1 -> (Class<Element>) e1.getModelInterface()));
  }

  private <T extends Element> void prepareAndSaveElements(
      Class<T> entityType, List<T> elementsWithType, Unit demoUnit, AtomicInteger counter) {
    elementsWithType.forEach(element -> prepareElement(element, demoUnit, counter));
    saveElements(entityType, elementsWithType);
  }

  private <T extends Element> void saveElements(Class<T> entityType, List<T> elementsWithType) {
    ElementRepository<T> elementRepository = repositoryProvider.getElementRepositoryFor(entityType);
    log.debug("Saving {} entities with type {}", elementsWithType.size(), entityType);
    elementRepository.saveAll(Set.copyOf(elementsWithType));
    log.debug("Done");
  }

  private void prepareElement(Element element, Unit demoUnit, AtomicInteger counter) {
    log.debug("Preparing element {}:{}", element.getId(), element);
    element.setDesignator(DEMO_UNIT_DESIGNATOR_PREFIX + counter.incrementAndGet());
    element.setOwner(demoUnit);

    if (element instanceof CompositeElement<?>) {
      CompositeElement<?> ce = (CompositeElement<?>) element;
      ce.getParts().forEach(e -> prepareElement(e, demoUnit, counter));
    } else if (element instanceof Scope) {
      Scope scope = (Scope) element;
      Set<Element> members = scope.getMembers();
      members.forEach(m -> prepareElement(m, demoUnit, counter));
    }
  }

  @Valid
  @Value
  public static class InputData implements UseCase.InputData {
    Key<UUID> clientId;
  }

  @Valid
  @Value
  public static class OutputData implements UseCase.OutputData {
    @Valid Unit unit;
  }
}
