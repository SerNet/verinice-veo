/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Jonas Jordan.
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
package org.veo.jobs;

import static org.veo.rest.VeoRestConfiguration.PROFILE_BACKGROUND_TASKS;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import org.veo.core.entity.event.StoredEvent;
import org.veo.message.EventDispatcher;
import org.veo.persistence.access.StoredEventRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Deletes {@link StoredEvent}s from the DB once they've been acked. Listens to acks on the {@link
 * EventDispatcher} and collects acked messages for batch deletion.
 */
@Component
@Slf4j
@Profile(PROFILE_BACKGROUND_TASKS)
public class MessageDeletionJob {
  private final StoredEventRepository storedEventRepository;
  private final Set<Long> ackedMessageIds = ConcurrentHashMap.newKeySet();

  @Autowired
  public MessageDeletionJob(
      StoredEventRepository storedEventRepository, EventDispatcher eventDispatcher) {
    log.debug("Creating MessageDeletionJob");
    this.storedEventRepository = storedEventRepository;
    eventDispatcher.addAckCallback(ackedMessageIds::add);
  }

  @Scheduled(fixedDelayString = "${veo.messages.deletion.delayMs:500}")
  public void deleteMessages() {
    var messageIdsToDelete = new HashSet<>(ackedMessageIds);
    if (!messageIdsToDelete.isEmpty()) {
      new EventDeleter().delete(messageIdsToDelete);
      ackedMessageIds.removeAll(messageIdsToDelete);
    } else {
      log.debug("Nothing to delete");
    }
  }

  /**
   * An inner class is used to wrap event deletion in a dedicated transaction. This is due to
   * limitations of Spring AOP:
   *
   * <p>"In proxy mode (which is the default), only external method calls coming in through the
   * proxy are intercepted. This means that self-invocation, in effect, a method within the target
   * object calling another method of the target object, will not lead to an actual transaction at
   * runtime even if the invoked method is marked with @Transactional."
   */
  public class EventDeleter {
    @Transactional
    public void delete(Set<Long> messageIds) {
      log.info("Deleting {} acked messages", messageIds.size());
      storedEventRepository.delete(messageIds);
    }
  }
}
