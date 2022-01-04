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

import javax.persistence.PrePersist;
import javax.persistence.PreRemove;
import javax.persistence.PreUpdate;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import org.veo.core.entity.Identifiable;
import org.veo.core.entity.Versioned;
import org.veo.core.entity.event.VersioningEvent;
import org.veo.core.entity.event.VersioningEvent.Type;
import org.veo.persistence.CurrentUserProvider;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Listens to JPA events on {@link VersionedData} objects and delegates them to
 * the {@link ApplicationEventPublisher} as {@link VersioningEvent}s.
 */
@Slf4j
@AllArgsConstructor
public class VersionedEntityListener {
    private final ApplicationEventPublisher publisher;
    private final CurrentUserProvider currentUserProvider;

    @PrePersist
    public void prePersist(Versioned entity) {
        log.debug("Publishing PrePersist event for {}", entity);
        if (entity instanceof Identifiable) {
            // We need to fire this one a little later (after the entity's ID has been
            // generated).
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void beforeCommit(boolean readOnly) {
                    publisher.publishEvent(new VersioningEvent(entity, Type.PERSIST,
                            currentUserProvider.getUsername()));
                }
            });
        } else {
            publisher.publishEvent(new VersioningEvent(entity, Type.PERSIST,
                    currentUserProvider.getUsername()));
        }
    }

    @PreUpdate
    public void preUpdate(Versioned entity) {
        publisher.publishEvent(new VersioningEvent(entity, Type.UPDATE,
                currentUserProvider.getUsername()));
    }

    @PreRemove
    public void preRemove(Versioned entity) {
        publisher.publishEvent(new VersioningEvent(entity, Type.REMOVE,
                currentUserProvider.getUsername()));
    }
}
