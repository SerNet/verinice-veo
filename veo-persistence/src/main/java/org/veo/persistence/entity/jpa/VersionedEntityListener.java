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

import static java.lang.String.join;
import static org.veo.core.entity.event.VersioningEvent.ModificationType.PERSIST;
import static org.veo.core.entity.event.VersioningEvent.ModificationType.REMOVE;
import static org.veo.core.entity.event.VersioningEvent.ModificationType.UPDATE;

import java.time.Instant;
import java.util.Arrays;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import org.veo.core.entity.Client;
import org.veo.core.entity.ClientOwned;
import org.veo.core.entity.Identifiable;
import org.veo.core.entity.Versioned;
import org.veo.core.entity.event.ClientOwnedEntityVersioningEvent;
import org.veo.core.entity.event.ClientVersioningEvent;
import org.veo.core.entity.event.VersioningEvent.ModificationType;
import org.veo.persistence.CurrentUserProvider;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Listens to JPA events on {@link VersionedData} objects and delegates them to the {@link
 * ApplicationEventPublisher} as {@link org.veo.core.entity.event.VersioningEvent}s.
 */
@Slf4j
@AllArgsConstructor
public class VersionedEntityListener<T extends Versioned & ClientOwned> {
  private final ApplicationEventPublisher publisher;
  private final CurrentUserProvider currentUserProvider;

  @PrePersist
  public void prePersist(Versioned entity) {
    log.debug("Publishing PrePersist event for {}", entity);
    var event =
        new ClientOwnedEntityVersioningEvent<>(
            (T) entity, PERSIST, currentUserProvider.getUsername(), Instant.now());
    if (entity instanceof Identifiable) {
      // We need to fire this one a little later (after the entity's ID has been
      // generated).
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void beforeCommit(boolean readOnly) {
              publish(entity, PERSIST);
            }
          });
    } else {
      publish(entity, PERSIST);
    }
  }

  @PreUpdate
  public void preUpdate(Versioned entity) {
    publish(entity, UPDATE);
  }

  @PreRemove
  public void preRemove(Versioned entity) {
    publish(entity, REMOVE);
  }

  private void publish(Versioned entity, ModificationType type) {
    // Since JPA does not honor generics we can receive any type of "Versioned" here.
    // This includes instances that do not implement <T>.
    if (entity instanceof Client client) {
      var event = new ClientVersioningEvent(client, type);
      publisher.publishEvent(event);
    } else if (entity instanceof ClientOwned co) {
      var event =
          new ClientOwnedEntityVersioningEvent<>(
              (T) co, type, currentUserProvider.getUsername(), Instant.now());
      publisher.publishEvent(event);
    } else {
      log.warn(
          "Cannot create versioning event for unsupported type: {}. ",
          join(", ", Arrays.toString(entity.getClass().getGenericInterfaces())));
    }
  }
}
