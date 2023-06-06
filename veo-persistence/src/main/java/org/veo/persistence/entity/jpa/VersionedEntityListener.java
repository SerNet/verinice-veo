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

import org.veo.core.entity.Client;
import org.veo.core.entity.ClientOwned;
import org.veo.core.entity.Versioned;
import org.veo.core.entity.event.ClientOwnedEntityVersioningEvent;
import org.veo.core.entity.event.ClientVersioningEvent;
import org.veo.core.entity.event.VersioningEvent;
import org.veo.core.entity.event.VersioningEvent.ModificationType;
import org.veo.persistence.CurrentUserProvider;
import org.veo.persistence.access.jpa.MostRecentChangeTracker;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Listens to JPA events on {@link VersionedData} objects and delegates them to the {@link
 * ApplicationEventPublisher} as {@link VersioningEvent}s.
 */
@Slf4j
@AllArgsConstructor
public class VersionedEntityListener<T extends Versioned & ClientOwned> {
  private final ApplicationEventPublisher publisher;
  private final CurrentUserProvider currentUserProvider;

  @PrePersist
  public void prePersist(Versioned entity) {
    // we need to capture the change number for this event NOW since - because
    // of the delayed publishing before-commit other change numbers may have
    // already been produced for updates before we get to store it:
    var insertChangeNumber = entity.initialChangeNumberForInsert();
    log.debug(
        "Publishing PrePersist event for {} with changeNumber {}", entity, insertChangeNumber);
    collectForPublishing(entity, PERSIST, insertChangeNumber);
  }

  @PreUpdate
  public void preUpdate(Versioned entity) {
    var changeNumber = entity.nextChangeNumberForUpdate();
    log.debug("Publishing PreUpdate event for {} with changeNumber {}", entity, changeNumber);
    collectForPublishing(entity, UPDATE, changeNumber);
  }

  @PreRemove
  public void preRemove(Versioned entity) {
    var changeNumber = entity.nextChangeNumberForUpdate();
    log.debug("Publishing PreRemove event for {} with change number {}", entity, changeNumber);
    collectForPublishing(entity, REMOVE, changeNumber);
  }

  private void collectForPublishing(Versioned entity, ModificationType type, long changeNumber) {
    if (entity instanceof Client client) {
      var event = new ClientVersioningEvent(client, type, changeNumber);
      // publish this right away: the client is a prerequisite for most other operations
      publisher.publishEvent(event);
    } else if (entity instanceof ClientOwned co) {
      var event =
          new ClientOwnedEntityVersioningEvent<>(
              (T) co, type, currentUserProvider.getUsername(), Instant.now(), changeNumber);
      // collect these events and publish them later:
      MostRecentChangeTracker.getForCurrentTransaction(publisher).put(event);
    } else {
      log.warn(
          "Cannot create versioning event for unsupported type: {}. ",
          join(", ", Arrays.toString(entity.getClass().getGenericInterfaces())));
    }
  }
}
