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
import org.veo.core.entity.Key;
import org.veo.core.entity.riskdefinition.RiskDefinition;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;

/**
 * An event that is triggered for a unit and all it associated domains, or a domain and all its
 * units it is associated with.
 */
@Value
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class RiskDefinitionChangedEvent implements DomainEvent {
  Domain domain;
  RiskDefinition riskDefinition;
  Client client;
  boolean migrateElements;
  Object source;

  public static RiskDefinitionChangedEvent from(
      Domain domain, RiskDefinition riskDefinition, Object source) {
    return new RiskDefinitionChangedEvent(
        domain, riskDefinition, domain.getOwningClient().orElseThrow(), true, source);
  }

  public static RiskDefinitionChangedEvent from(
      Domain domain, RiskDefinition riskDefinition, boolean migrateElements, Object source) {
    return new RiskDefinitionChangedEvent(
        domain, riskDefinition, domain.getOwningClient().orElseThrow(), migrateElements, source);
  }

  @Override
  public Key<UUID> getClientId() {
    return client.getId();
  }
}
