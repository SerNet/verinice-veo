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

import javax.annotation.Nullable;

import org.veo.core.entity.AbstractRisk;
import org.veo.core.entity.Key;
import org.veo.core.entity.risk.RiskDefinitionRef;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.With;

/** An event that is triggered when a risk-value has been re-calculated by the risk service. */
@Value
@RequiredArgsConstructor
@With
public class RiskChangedEvent implements RiskEvent, DomainEvent {

  public RiskChangedEvent(AbstractRisk<?, ?> risk, Object source) {
    this.risk = risk;
    this.source = source;
    this.changes.addAll(
        Set.of(PROBABILITY_VALUES_CHANGED, IMPACT_VALUES_CHANGED, RISK_VALUES_CHANGED));
    this.clientId = risk.getEntity().getOwningClient().orElseThrow().getId();
    this.domainId = null;
    this.riskDefinition = null;
  }

  @Getter(AccessLevel.NONE)
  AbstractRisk<?, ?> risk;

  Object source;

  Key<UUID> clientId;

  Set<RiskEvent.ChangedValues> changes = new HashSet<>();

  public void addChange(ChangedValues change) {
    changes.add(change);
  }

  /** The affected domain. May be null, i.e. when deleting a risk which affects all domains. */
  @Nullable Key<UUID> domainId;

  /**
   * The affected risk -definition. May be null, i.e. when deleting a risk which affects all risk
   * definitions.
   */
  @Nullable RiskDefinitionRef riskDefinition;

  public Key<UUID> getRiskAffectedId() {
    return risk.getEntity().getId();
  }

  public Key<UUID> getScenarioId() {
    return risk.getScenario().getId();
  }
}
