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
package org.veo.jobs;

import static org.veo.message.EventMessage.messagesFrom;
import static org.veo.rest.VeoRestConfiguration.PROFILE_BACKGROUND_TASKS;

import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import org.veo.core.entity.event.StoredEvent;
import org.veo.message.EventDispatcher;
import org.veo.persistence.access.StoredEventRepository;
import org.veo.rest.VeoRestConfiguration;

import lombok.extern.slf4j.Slf4j;

/** Forwards new {@link StoredEvent}s to the external message queue. */
@Component
@Slf4j
@Profile(PROFILE_BACKGROUND_TASKS)
public class MessagingJob {
  @Value("${veo.messages.publishing.processingChunkSize:5000}")
  public int processingChunkSize;

  @Value("${veo.message.exchanges.veo}")
  private String exchange;

  private final StoredEventRepository storedEventRepository;

  private final VeoRestConfiguration config;

  private final EventDispatcher eventDispatcher;

  public MessagingJob(
      StoredEventRepository storedEventRepository,
      VeoRestConfiguration config,
      EventDispatcher eventDispatcher) {
    this.storedEventRepository = storedEventRepository;
    this.config = config;
    this.eventDispatcher = eventDispatcher;
  }

  @Scheduled(fixedDelayString = "${veo.messages.publishing.delayMs:200}")
  public void sendMessages() {
    var retriever = new EventRetriever();
    List<StoredEvent> pendingEvents = retriever.retrievePendingEvents();
    if (pendingEvents.isEmpty()) return;
    log.debug("Dispatching messages for {} stored events.", pendingEvents.size());
    eventDispatcher.send(exchange, messagesFrom(pendingEvents));
  }

  /**
   * An inner class is used to wrap the event retrieval and locking in a dedicated transaction. See
   * the remarks on the EventSender class for a more detailed explanation.
   *
   * <p>The event retrieval needs to run in its own read-write transaction because the retrieved
   * events are time-locked to prevent another MessagingJob from working on the same retrieved
   * events. This needs to be an atomic transaction that is committed before this MessagingJob
   * begins working on the retrieved events.
   */
  public class EventRetriever {
    @Transactional
    public List<StoredEvent> retrievePendingEvents() {
      var events =
          storedEventRepository.findPendingEvents(
              Instant.now().minus(config.getMessagePublishingLockExpiration()),
              processingChunkSize);
      events.forEach(StoredEvent::lock);
      storedEventRepository.saveAll(events);
      return events;
    }
  }
}
