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

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import org.veo.core.entity.Element;
import org.veo.core.entity.event.ElementEvent;
import org.veo.core.entity.event.RiskAffectingElementChangeEvent;
import org.veo.core.repository.ElementRepository;
import org.veo.core.repository.RepositoryProvider;
import org.veo.core.usecase.decision.Decider;
import org.veo.service.risk.RiskService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Listens to {@link RiskAffectingElementChangeEvent}s from the use-case layer and invokes the
 * {@link RiskService}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RiskComponentChangeListener {
  private final RiskService riskService;
  private final RepositoryProvider repositoryProvider;
  private final Decider decider;

  @TransactionalEventListener(condition = "#event.source != @riskService")
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void handle(RiskAffectingElementChangeEvent event) {
    ElementRepository<? extends Element> repository =
        repositoryProvider.getElementRepositoryFor(event.getEntityType());
    Element element = repository.findById(event.getEntityId()).orElseThrow();
    riskService.evaluateChangedRiskComponent(element);
  }

  @TransactionalEventListener
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void handle(ElementEvent event) {
    decider.updateDecisions(event);
  }
}
