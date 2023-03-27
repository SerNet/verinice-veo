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

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import org.veo.core.entity.event.StoredEvent;
import org.veo.message.EventDispatcher;
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

  private final VeoRestConfiguration config;

  private final EventDispatcher eventDispatcher;

  private final EventRetriever retriever;

  public MessagingJob(
      VeoRestConfiguration config, EventDispatcher eventDispatcher, EventRetriever retriever) {
    this.config = config;
    this.eventDispatcher = eventDispatcher;
    this.retriever = retriever;
  }

  @Scheduled(fixedDelayString = "${veo.messages.publishing.delayMs:500}")
  public void sendMessages() {
    List<StoredEvent> pendingEvents =
        retriever.retrievePendingEvents(
            config.getMessagePublishingLockExpiration(), processingChunkSize);
    if (pendingEvents.isEmpty()) return;
    log.info("Dispatching messages for {} stored events.", pendingEvents.size());
    eventDispatcher.send(exchange, messagesFrom(pendingEvents));
  }
}
