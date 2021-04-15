/*******************************************************************************
 * Copyright (c) 2021 Jonas Jordan.
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.veo;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import org.veo.core.entity.event.StoredEvent;
import org.veo.message.EventDispatcher;
import org.veo.message.EventMessage;
import org.veo.persistence.access.StoredEventRepository;
import org.veo.rest.VeoRestConfiguration;

import lombok.extern.slf4j.Slf4j;

/**
 * Forwards new {@link StoredEvent}s to the external message queue.
 */
@Component
@Slf4j
@Profile("publishing-enabled")
public class MessagingJob {

    /**
     * Keep transaction open and wait until all events are acknowledged - but no
     * longer than the configured wait time. Unacknowledged events will be sent
     * again during the next publication after teir established lock time.
     */
    @Value("${veo.messages.publishing.confirmationWaitMs:2000}")
    public int CONFIRMATION_WAIT;

    private final StoredEventRepository storedEventRepository;

    private final VeoRestConfiguration config;

    private final EventDispatcher eventDispatcher;

    public MessagingJob(StoredEventRepository storedEventRepository, VeoRestConfiguration config,
            EventDispatcher eventDispatcher) {
        this.storedEventRepository = storedEventRepository;
        this.config = config;
        this.eventDispatcher = eventDispatcher;
    }

    @Scheduled(fixedRateString = "${veo.messages.publishing.rateMs:2000}")
    public void sendMessages() {
        var sender = new EventSender();
        var retriever = new EventRetriever();
        sender.send(retriever.retrievePendingEvents());
    }

    /**
     * An inner class is used to wrap the event sender in a dedicated transaction.
     * This is due to limitations of Spring AOP:
     * <p>
     * "In proxy mode (which is the default), only external method calls coming in
     * through the proxy are intercepted. This means that self-invocation, in
     * effect, a method within the target object calling another method of the
     * target object, will not lead to an actual transaction at runtime even if the
     * invoked method is marked with @Transactional."
     */
    public class EventSender {
        @Transactional
        public void send(List<StoredEvent> pendingEvents) {
            if (pendingEvents.isEmpty())
                return;

            log.debug("Dispatching messages for {} stored events.", pendingEvents.size());
            var latch = new CountDownLatch(pendingEvents.size());
            pendingEvents.forEach(e -> {
                eventDispatcher.sendAsync(EventMessage.from(e), (ack) -> {
                    if (ack && e.markAsProcessed()) {
                        storedEventRepository.save(e);
                        latch.countDown();
                    } else
                        log.warn("Dispatch unsuccessful for stored event {}.", e.getId());
                });
            });

            // keep this transaction open until all messages are confirmed - but no longer
            // than CONFIRMATION_WAIT:
            try {
                if (!latch.await(CONFIRMATION_WAIT, TimeUnit.MILLISECONDS)) {
                    log.warn("Timeout reached before receiving ACK for all dispatched messages. "
                            + "{} remaining messages will not be marked as processed and "
                            + "re-sent during the next scheduled publication after the "
                            + "lock period of {} seconds", latch.getCount(),
                             config.getMessagePublishingLockExpiration()
                                   .getSeconds());
                } else {
                    log.debug("Success! Received ACK for all dispatched messages.");
                }
            } catch (InterruptedException e) {
                log.warn("Interrupted while waiting for confirmation from published events.", e);
            }
        }
    }

    /**
     * An inner class is used to wrap the event sender in a dedicated transaction.
     * See the remarks on the EventSender class for a more detailed explanation.
     * <p>
     * The event retrieval needs to run in its own read-write transaction because
     * the retrieved events are time-locked to prevent another MessagingJob from
     * working on the same retrieved events. This needs to be an atomic transaction
     * that is committed before this MessagingJob begins working on the retrieved
     * events.
     */
    public class EventRetriever {
        @Transactional
        public List<StoredEvent> retrievePendingEvents() {
            var events = storedEventRepository.findPendingEvents(Instant.now()
                                                                        .minus(config.getMessagePublishingLockExpiration()));
            events.forEach(StoredEvent::lock);
            return events;
        }
    }
}
