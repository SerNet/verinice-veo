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
package org.veo.core.entity.event;

import static org.veo.core.entity.event.RiskEvent.ChangedValues.IMPACT_VALUES_CHANGED;
import static org.veo.core.entity.event.RiskEvent.ChangedValues.PROBABILITY_VALUES_CHANGED;
import static org.veo.core.entity.event.RiskEvent.ChangedValues.RISK_VALUES_CHANGED;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.veo.core.entity.Element;
import org.veo.core.entity.Key;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.With;

/** An event that is triggered when a risk-relevant change is made to an entity. */
@Value
@RequiredArgsConstructor
@With
public class RiskAffectingElementChangeEvent implements RiskEvent, ElementEvent {

  public RiskAffectingElementChangeEvent(Element entity, Object source) {
    this.entity = entity;
    this.source = source;
    this.changes.addAll(
        Set.of(PROBABILITY_VALUES_CHANGED, IMPACT_VALUES_CHANGED, RISK_VALUES_CHANGED));
    this.clientId = entity.getOwningClient().orElseThrow().getId();
  }

  /**
   * This allows events to be published for entities that have not yet been persisted and whose ID
   * is therefore {@code null}. The ID will have been initialized however when the event listener
   * accesses it. NOTE: this requires that event listeners run only after successful commit. This is
   * the default for {@code TransactionalEventListener}.
   */
  @Getter(AccessLevel.NONE)
  Element entity;

  Key<UUID> clientId;

  public RiskAffectingElementChangeEvent(
      Element element, Object source, RiskChangedEvent riskEvent) {
    this(element, source);
    changedRisks.add(riskEvent);
  }

  public Key<UUID> getEntityId() {
    return entity.getId();
  }

  Object source;

  Set<ChangedValues> changes = new HashSet<>();

  Set<RiskChangedEvent> changedRisks = new HashSet<>();

  @Override
  @SuppressWarnings("unchecked")
  public Class<? extends Element> getEntityType() {
    return (Class<? extends Element>) entity.getModelInterface();
  }

  public void addChangedRisk(RiskChangedEvent riskEvent) {
    changedRisks.add(riskEvent);
  }

  public boolean hasChangedRisks() {
    return changedRisks.size() > 0;
  }

  @Override
  public Object getSource() {
    return source;
  }
}
