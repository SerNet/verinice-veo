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
package org.veo;

import org.apache.commons.lang3.NotImplementedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.veo.adapter.presenter.api.common.ReferenceAssembler;
import org.veo.adapter.presenter.api.response.transformer.EntityToDtoTransformer;
import org.veo.core.entity.AbstractRisk;
import org.veo.core.entity.Client;
import org.veo.core.entity.ClientOwned;
import org.veo.core.entity.ModelObject;
import org.veo.core.entity.Versioned;
import org.veo.core.entity.event.StoredEvent;
import org.veo.persistence.access.StoredEventRepository;
import org.veo.persistence.entity.jpa.StoredEventData;
import org.veo.persistence.entity.jpa.VersioningEvent;
import org.veo.persistence.entity.jpa.VersioningEvent.Type;

import lombok.extern.slf4j.Slf4j;

/**
 * Listens to {@link VersioningEvent}s from the persistence layer and saves them
 * as {@link StoredEvent}s.
 */
@Component
@Slf4j
public class VersioningEventListener {

    @Value("${veo.message.dispatch.routing-key-prefix}")
    private String ROUTING_KEY_PREFIX;

    private static final String ROUTING_KEY = "versioning_event";

    private final ObjectMapper objectMapper;
    private final StoredEventRepository storedEventRepository;
    private final ReferenceAssembler referenceAssembler;
    private final EntityToDtoTransformer entityToDtoTransformer;

    public VersioningEventListener(ObjectMapper objectMapper,
            StoredEventRepository storedEventRepository, ReferenceAssembler referenceAssembler,
            EntityToDtoTransformer entityToDtoTransformer) {
        this.objectMapper = objectMapper;
        this.storedEventRepository = storedEventRepository;
        this.referenceAssembler = referenceAssembler;
        this.entityToDtoTransformer = entityToDtoTransformer;
    }

    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    void handleVersioningEvent(VersioningEvent event) {
        Versioned entity = event.getEntity();
        if (!(entity instanceof ClientOwned)) {
            log.debug("Ignoring event for entity {}", entity);
            return;
        }
        Type eventType = event.getType();
        var client = ((ClientOwned) entity).getOwningClient();
        if (client.isEmpty()) {
            log.debug("Ignoring {} event for entity {} which does not belong to a client",
                      eventType, entity);
            return;
        }
        log.debug("Storing {} event for entity {} modified by user {}", event.getType(), entity,
                  event.getAuthor());
        var storedEvent = StoredEventData.newInstance(createJson(entity, eventType,
                                                                 event.getAuthor(), client.get()),
                                                      ROUTING_KEY_PREFIX + ROUTING_KEY);
        storedEventRepository.save(storedEvent);
    }

    private String createJson(Versioned entity, VersioningEvent.Type type, String author,
            Client client) {
        var tree = objectMapper.createObjectNode();
        tree.put("uri", getUri(entity));
        tree.put("type", convertType(type));
        tree.put("changeNumber", getChangeNumber(entity, type));
        tree.put("time", entity.getUpdatedAt()
                               .toString());
        tree.put("author", author);
        tree.put("clientId", client.getId()
                                   .uuidValue());

        if (type != VersioningEvent.Type.REMOVE) {
            tree.set("content",
                     objectMapper.valueToTree(entityToDtoTransformer.transform2Dto(entity)));
        }
        return tree.toString();
    }

    private long getChangeNumber(Versioned entity, VersioningEvent.Type type) {
        // We use the JPA version number as a base for our continuous change number.
        // When updating an entity, JPA increments the version number after this event
        // creation, so we must add 1 to the version number. We must also add 1 in case
        // of a deletion, because JPA won't assign a new number for a deleted entity.
        var changeNumber = entity.getVersion();
        if (type != VersioningEvent.Type.PERSIST) {
            changeNumber++;
        }
        return changeNumber;
    }

    private String getUri(Versioned entity) {
        if (entity instanceof ModelObject) {
            return referenceAssembler.targetReferenceOf((ModelObject) entity);
        }
        if (entity instanceof AbstractRisk) {
            return referenceAssembler.targetReferenceOf((AbstractRisk<?, ?>) entity);
        }
        throw new NotImplementedException(
                "Can't build URI for object of type " + entity.getClass());
    }

    private String convertType(VersioningEvent.Type type) {
        switch (type) {
        case PERSIST:
            return "CREATION";
        case UPDATE:
            return "MODIFICATION";
        case REMOVE:
            return "HARD_DELETION";
        }
        throw new NotImplementedException("Event type " + type + " not supported.");
    }
}