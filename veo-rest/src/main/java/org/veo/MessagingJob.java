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

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import org.veo.core.entity.StoredEvent;
import org.veo.persistence.access.StoredEventRepository;
import org.veo.rest.VeoRestConfiguration;

/**
 * Forwards new {@link StoredEvent}s to the external message queue.
 */
@Component
public class MessagingJob {
    private final StoredEventRepository storedEventRepository;
    private final VeoRestConfiguration config;

    public MessagingJob(StoredEventRepository storedEventRepository, VeoRestConfiguration config) {
        this.storedEventRepository = storedEventRepository;
        this.config = config;
    }

    @Scheduled(fixedRateString = "${veo.messages.publishing.rateMs:2000}")
    public void sendMessages() {
        send(retrievePendingEvents());
    }

    @Transactional
    private void send(List<StoredEvent> pendingEvents) {
        if (!pendingEvents.isEmpty()) {
            // TODO VEO-453 publish
            pendingEvents.forEach(StoredEvent::setProcessed);
        }
    }

    @Transactional
    public List<StoredEvent> retrievePendingEvents() {
        var events = storedEventRepository.findPendingEvents(Instant.now()
                                                                    .minus(config.getMessagePublishingLockExpiration()));
        events.forEach(StoredEvent::lock);
        return events;
    }
}
