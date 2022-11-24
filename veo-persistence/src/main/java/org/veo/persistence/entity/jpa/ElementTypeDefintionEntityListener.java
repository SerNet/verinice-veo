/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Jonas Jordan.
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
package org.veo.persistence.entity.jpa;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import org.springframework.context.ApplicationEventPublisher;

import org.veo.core.entity.Domain;
import org.veo.core.entity.event.ElementTypeDefinitionUpdateEvent;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Listens to JPA events on {@link ElementTypeDefinitionData} objects and publishes {@link
 * ElementTypeDefinitionUpdateEvent}s using the {@link ApplicationEventPublisher}.
 */
@Slf4j
@AllArgsConstructor
public class ElementTypeDefintionEntityListener {
  private final ApplicationEventPublisher publisher;

  @PrePersist
  public void prePersist(ElementTypeDefinitionData definition) {
    // When an existing element type definition is replaced, PrePersist is triggered
    // instead of PreUpdate. But we can use the owner's version to detect if it is a
    // creation or an update (assuming that element type definitions are always
    // created in the same transaction as their owner).
    if (definition.getOwner() instanceof Domain domain && definition.getOwner().getVersion() > 0) {
      publisher.publishEvent(new ElementTypeDefinitionUpdateEvent(definition, domain));
    }
  }

  @PreUpdate
  public void preUpdate(ElementTypeDefinitionData definition) {
    if (definition.getOwner() instanceof Domain domain) {
      publisher.publishEvent(new ElementTypeDefinitionUpdateEvent(definition, domain));
    }
  }
}
