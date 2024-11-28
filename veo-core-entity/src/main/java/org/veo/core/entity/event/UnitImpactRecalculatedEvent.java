/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2024  Urs Zeidler
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

import java.util.UUID;

import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Unit;

import lombok.Value;

/**
 * An event that is triggered for a unit and all it associated domains, or a domain and all its
 * units it is associated with.
 */
@Value
public class UnitImpactRecalculatedEvent implements DomainEvent {
  Unit unit;
  Domain domain;
  Client client;
  Object source;

  private UnitImpactRecalculatedEvent(Unit unit, Domain domain, Client client, Object source) {
    this.unit = unit;
    this.domain = domain;
    this.source = source;
    this.client = client;
  }

  /** Recalculate the impact for the unit in all domains associated with this unit. * */
  public static UnitImpactRecalculatedEvent from(Unit unit, Object source) {
    return new UnitImpactRecalculatedEvent(
        unit, null, unit.getOwningClient().orElseThrow(), source);
  }

  /** Recalculate the impact for the unit in the domain. * */
  public static UnitImpactRecalculatedEvent from(Unit unit, Domain domain, Object source) {
    return new UnitImpactRecalculatedEvent(
        unit, domain, domain.getOwningClient().orElseThrow(), source);
  }

  @Override
  public UUID getClientId() {
    return client.getId();
  }
}
