/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Jonas Jordan
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
package org.veo.core.events;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;

import org.veo.persistence.access.StoredEventRepository;
import org.veo.persistence.entity.jpa.StoredEventData;

import lombok.RequiredArgsConstructor;

/**
 * Persists events for message publishing. Handles routing key composition.
 */
@RequiredArgsConstructor
@Component
public class StoredEventService {
    private final StoredEventRepository storedEventRepository;

    @Value("${veo.message.dispatch.routing-key-prefix}")
    private String routingKeyPrefix;

    public void storeEvent(String routingKey, JsonNode content) {
        storedEventRepository.save(StoredEventData.newInstance(content.toString(),
                                                               routingKeyPrefix + routingKey));
    }
}
