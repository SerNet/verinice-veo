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
package org.veo.listeners;

import static org.springframework.transaction.annotation.Propagation.MANDATORY;
import static org.veo.core.entity.ClientState.DELETED;
import static org.veo.core.entity.event.VersioningEvent.ModificationType.PERSIST;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import org.veo.core.entity.ClientOwned;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Versioned;
import org.veo.core.entity.event.ClientOwnedEntityVersioningEvent;
import org.veo.core.entity.event.StoredEvent;
import org.veo.core.usecase.MessageCreator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Listens to {@link ClientOwnedEntityVersioningEvent}s from the persistence layer and saves them as
 * {@link StoredEvent}s.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class VersioningEventListener {
  private final MessageCreator messageCreator;

  @EventListener
  @Transactional(propagation = MANDATORY)
  public <T extends Versioned & ClientOwned> void handle(
      ClientOwnedEntityVersioningEvent<T> event) {

    log.debug(
        "Handling VersioningEvent of type {} with changeNo {} for {}",
        event.getType(),
        event.getChangeNumber(),
        event.getEntity().getClass());
    var entity = event.getEntity();

    if (entity instanceof Domain domain) {
      if (event.getType() == PERSIST) {
        log.debug("Creating domain creation message for domain {}}", domain.getIdAsString());
        messageCreator.createDomainCreationMessage(domain);
      }
      // Do not create entity revisions for domains (they are too large)
      return;
    }

    entity
        .getOwningClient()
        .ifPresent(
            client -> {
              if (client.getState() == DELETED) {
                log.debug(
                    "Ignoring entity revision event for deleted client {}", client.getIdAsString());
                return;
              }
              log.debug(
                  "Creating entity revision message for {} event for entity {} modified by user {}",
                  event.getType(),
                  entity,
                  event.getAuthor());
              messageCreator.createEntityRevisionMessage(event);
            });
  }
}
