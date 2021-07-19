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

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.veo.core.entity.Domain;
import org.veo.core.entity.Versioned;
import org.veo.core.events.StoredEventService;
import org.veo.persistence.entity.jpa.VersioningEvent;
import org.veo.persistence.entity.jpa.VersioningEvent.Type;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Listens to {@link VersioningEvent}s from the persistence layer and stores
 * domain related events for publishing.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DomainEventListener {
    private static final String DOMAIN_CREATION_ROUTING_KEY = "domain_creation_event";

    private final ObjectMapper objectMapper;
    private final StoredEventService storedEventService;

    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    void handleVersioningEvent(VersioningEvent event) {
        Versioned entity = event.getEntity();
        if (!(entity instanceof Domain) || event.getType() != Type.PERSIST) {
            log.debug("Ignoring event for entity {}", entity);
            return;
        }
        var domain = (Domain) entity;
        log.debug("Storing domain event for {}}", domain);
        storedEventService.storeEvent(DOMAIN_CREATION_ROUTING_KEY, createJson(domain));
    }

    private JsonNode createJson(Domain domain) {
        var tree = objectMapper.createObjectNode();
        tree.put("domainId", domain.getId()
                                   .uuidValue());
        tree.put("clientId", domain.getOwner()
                                   .getId()
                                   .uuidValue());
        if (domain.getDomainTemplate() != null) {
            tree.put("domainTemplateId", domain.getDomainTemplate()
                                               .getId()
                                               .uuidValue());
        }
        return tree;
    }
}
