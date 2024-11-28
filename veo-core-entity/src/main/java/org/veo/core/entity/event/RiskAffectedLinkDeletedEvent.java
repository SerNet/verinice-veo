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

import static org.veo.core.entity.event.RiskEvent.ChangedValues.IMPACT_VALUES_CHANGED;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Value;

/** An event that is triggered for the target entity when a risk-relevant link is deleted. */
@Value
@RequiredArgsConstructor
public class RiskAffectedLinkDeletedEvent implements DomainEvent, RiskEvent {
  @Getter(AccessLevel.NONE)
  Element entity;

  Domain domain;
  UUID clientId;
  Object source;
  String linkType;
  Set<ChangedValues> changes = new HashSet<>();

  public RiskAffectedLinkDeletedEvent(
      Element entity, Domain domain, String linkType, Object source) {
    this.entity = entity;
    this.source = source;
    this.domain = domain;
    this.linkType = linkType;
    this.changes.add(IMPACT_VALUES_CHANGED);

    this.clientId = entity.getOwningClient().orElseThrow().getId();
  }

  public UUID getEntityId() {
    return entity.getId();
  }

  @SuppressWarnings("unchecked")
  public Class<? extends Element> getEntityType() {
    return (Class<? extends Element>) entity.getModelInterface();
  }

  @Override
  public Set<ChangedValues> getChanges() {
    return changes;
  }
}
