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
import static org.veo.core.entity.event.RiskEvent.ChangedValues.RISK_CREATED;
import static org.veo.core.entity.event.RiskEvent.ChangedValues.RISK_DELETED;
import static org.veo.core.entity.event.RiskEvent.ChangedValues.RISK_VALUES_CHANGED;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import org.veo.core.entity.AbstractRisk;
import org.veo.core.entity.Identifiable;
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
public class RiskChangedEvent implements RiskEvent {

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

  UUID clientId;

  Set<RiskEvent.ChangedValues> changes = EnumSet.noneOf(RiskEvent.ChangedValues.class);

  public void addChange(ChangedValues change) {
    changes.add(change);
  }

  /** The affected domain. May be null, i.e. when deleting a risk which affects all domains. */
  @Nullable UUID domainId;

  /**
   * The affected risk-definition. May be null, i.e. when deleting a risk which affects all risk
   * definitions.
   */
  @Nullable RiskDefinitionRef riskDefinition;

  public UUID getRiskAffectedId() {
    return risk.getEntity().getId();
  }

  public UUID getScenarioId() {
    return risk.getScenario().getId();
  }

  /**
   * Test whether risk values should be re-evaluated because of changes. This is always the case
   * when a risk was created or removed.
   *
   * <p>For changes, this method only returns true if they apply to the requested domain.
   *
   * @param domain The domain to check
   * @return true if the risk should be re-evaluated, false otherwise.
   */
  public boolean shouldReevaluate(Identifiable domain) {
    var changes = getChanges();
    // Reevaluate max risk if a risk has been created or removed.
    if (changes.contains(RISK_CREATED) || changes.contains(RISK_DELETED)) {
      return true;
    }
    // Reevaluate max risk if a risk value has been changed in the domain.
    return changes.contains(RISK_VALUES_CHANGED) && Objects.equals(getDomainId(), domain.getId());
  }
}
