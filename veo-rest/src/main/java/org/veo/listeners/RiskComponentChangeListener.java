/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Jochen Kemnade.
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
package org.veo.listeners;

import static org.veo.core.entity.riskdefinition.RiskDefinitionChange.requiresImpactInheritanceRecalculation;
import static org.veo.core.entity.riskdefinition.RiskDefinitionChange.requiresMigration;
import static org.veo.core.entity.riskdefinition.RiskDefinitionChange.requiresRiskRecalculation;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import org.veo.core.entity.Asset;
import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;
import org.veo.core.entity.Process;
import org.veo.core.entity.RiskAffected;
import org.veo.core.entity.RiskRelated;
import org.veo.core.entity.Scope;
import org.veo.core.entity.Unit;
import org.veo.core.entity.event.ElementEvent;
import org.veo.core.entity.event.RiskAffectedLinkDeletedEvent;
import org.veo.core.entity.event.RiskAffectingElementChangeEvent;
import org.veo.core.entity.event.RiskDefinitionChangedEvent;
import org.veo.core.entity.event.RiskEvent.ChangedValues;
import org.veo.core.entity.event.UnitImpactRecalculatedEvent;
import org.veo.core.entity.riskdefinition.RiskDefinition;
import org.veo.core.repository.ElementQuery;
import org.veo.core.repository.GenericElementRepository;
import org.veo.core.repository.PagingConfiguration;
import org.veo.core.repository.QueryCondition;
import org.veo.core.repository.UnitRepository;
import org.veo.core.usecase.decision.Decider;
import org.veo.service.ElementMigrationService;
import org.veo.service.TemplateItemMigrationService;
import org.veo.service.risk.ImpactInheritanceCalculator;
import org.veo.service.risk.RiskService;

import lombok.RequiredArgsConstructor;

/**
 * Listens to {@link RiskAffectingElementChangeEvent}s from the use-case layer and invokes the
 * {@link RiskService}.
 */
@Component
@RequiredArgsConstructor
public class RiskComponentChangeListener {
  private final RiskService riskService;
  private final ImpactInheritanceCalculator impactInheritanceCalculator;
  private final GenericElementRepository elementRepository;
  private final UnitRepository unitRepository;
  private final Decider decider;
  private final ElementMigrationService elementMigrationService;
  private final TemplateItemMigrationService templateItemMigrationService;

  @TransactionalEventListener(condition = "#event.source != @riskService")
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void handle(RiskAffectingElementChangeEvent event) {
    elementRepository
        .findById(event.getEntityId(), event.getEntityType(), event.getClientId())
        .ifPresent(
            element -> {
              riskService.evaluateChangedRiskComponent(element);
              if (event.getChanges().contains(ChangedValues.IMPACT_VALUES_CHANGED)) {
                if (element instanceof RiskAffected<?, ?> ra) {
                  impactInheritanceCalculator.calculateImpactInheritance(ra);
                }
              }
            });
  }

  @TransactionalEventListener(condition = "#event.source != @riskService")
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void handle(RiskAffectedLinkDeletedEvent event) {
    Element element =
        elementRepository.getById(event.getEntityId(), event.getEntityType(), event.getClientId());

    if (element instanceof RiskAffected<?, ?> ra) {
      impactInheritanceCalculator.calculateImpactInheritance(
          ra, event.getDomain(), event.getLinkType());
    }
  }

  @TransactionalEventListener(condition = "#event.source != @riskService")
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void handle(UnitImpactRecalculatedEvent event) {
    Unit unit = unitRepository.findById(event.getUnit().getId()).orElseThrow();
    Optional.ofNullable(event.getDomain())
        .ifPresentOrElse(
            domain -> impactInheritanceCalculator.updateAllRootNodes(unit, domain),
            () -> impactInheritanceCalculator.updateAllRootNodes(unit));
  }

  @TransactionalEventListener(condition = "#event.source != @riskService")
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void handle(RiskDefinitionChangedEvent event) {
    RiskDefinition rd = event.getRiskDefinition();
    Domain domain = event.getDomain();
    Client client = event.getClient();
    if (requiresMigration(event.getChanges())) {
      ElementQuery<Element> query = elementRepository.query(client);
      query.whereDomainsContain(domain);
      // TODO #3142 migrate scenarios
      query.whereElementTypeMatches(
          new QueryCondition<>(
              Set.of(Asset.SINGULAR_TERM, Process.SINGULAR_TERM, Scope.SINGULAR_TERM)));
      List<Element> elements = query.execute(PagingConfiguration.UNPAGED).getResultPage();
      elements.forEach(
          e -> elementMigrationService.migrateRiskRelated((RiskRelated) e, domain, rd));

      templateItemMigrationService.migrateRiskDefinitionChange(domain);
    }
    if (requiresImpactInheritanceRecalculation(event.getChanges())
        && impactInheritanceCalculator.hasInheritingLinks().test(rd)) {
      List<Unit> units = unitRepository.findByDomain(domain.getId());
      units.stream()
          .forEach(
              unit -> {
                impactInheritanceCalculator.updateAllRootNodes(unit, domain, rd.getId());
              });
    }
    if (requiresRiskRecalculation(event.getChanges())) {
      ElementQuery<Element> query = elementRepository.query(client);
      query.whereDomainsContain(domain);
      query.whereElementTypeMatches(
          new QueryCondition<>(
              Set.of(Asset.SINGULAR_TERM, Process.SINGULAR_TERM, Scope.SINGULAR_TERM)));
      List<Element> elements = query.execute(PagingConfiguration.UNPAGED).getResultPage();
      elements.forEach(riskService::evaluateChangedRiskComponent);
    }
  }

  @TransactionalEventListener
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void handle(ElementEvent event) {
    decider.updateDecisions(event);
  }
}
