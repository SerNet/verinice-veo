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

import static java.util.Comparator.comparingInt;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.transaction.Transactional;
import javax.validation.Valid;

import org.veo.core.entity.Client;
import org.veo.core.entity.Designated;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;
import org.veo.core.entity.Key;
import org.veo.core.entity.RiskAffected;
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

  public CreateDemoUnitUseCase(
      ClientRepository clientRepository,
      UnitRepository unitRepository,
      EntityFactory entityFactory,
      DomainTemplateService domainTemplateService,
      RepositoryProvider repositoryProvider,
      EventPublisher eventPublisher) {
    this.clientRepository = clientRepository;
    this.unitRepository = unitRepository;
    this.entityFactory = entityFactory;
    this.domainTemplateService = domainTemplateService;
    this.repositoryProvider = repositoryProvider;
    this.eventPublisher = eventPublisher;
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
            .collect(Collectors.toSet());
    demoUnit.addToDomains(domainsFromElements);
    unitRepository.save(demoUnit);

    // Assign sequential demo designators in deterministic order and assign owner.
    var riskAffectedElements = new HashSet<Element>();
    var counter = new AtomicInteger();
    demoUnitElements.stream()
        .sorted(Comparator.comparing(e -> e.getModelInterface().getSimpleName()))
        .forEach(
            e -> {
              designate(e, counter);
              e.setOwner(demoUnit);
            });

    // Designate risks
    demoUnitElements.stream()
        .filter(e -> e instanceof RiskAffected)
        .map(e -> (RiskAffected<?, ?>) e)
        .flatMap(riskAffected -> riskAffected.getRisks().stream())
        .forEach(
            risk -> {
              designate(risk, counter);
              riskAffectedElements.add(risk.getEntity());
            });

    // Batch-save elements per type. Risk-affected elements must be saved last (because risks may
    // hold references to non-risk-affected elements which must have been saved beforehand).
    demoUnitElements.stream()
        .collect(Collectors.groupingBy(e1 -> (Class<Element>) e1.getModelInterface()))
        .entrySet()
        .stream()
        .sorted(comparingInt(t -> RiskAffected.class.isAssignableFrom(t.getKey()) ? 1 : 0))
        .forEach(t -> saveElements(t.getKey(), t.getValue()));

    riskAffectedElements.forEach(
        it -> eventPublisher.publish(new RiskAffectingElementChangeEvent(it, this)));
    return demoUnit;
  }

  private void designate(Designated entity, AtomicInteger counter) {
    entity.setDesignator(DEMO_UNIT_DESIGNATOR_PREFIX + counter.incrementAndGet());
  }

  private <T extends Element> void saveElements(Class<T> entityType, List<T> elementsWithType) {
    ElementRepository<T> elementRepository = repositoryProvider.getElementRepositoryFor(entityType);
    log.debug("Saving {} entities with type {}", elementsWithType.size(), entityType);
    elementRepository.saveAll(Set.copyOf(elementsWithType));
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
