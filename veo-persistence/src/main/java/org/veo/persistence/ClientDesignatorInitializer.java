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
package org.veo.persistence;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import org.veo.core.entity.Client;
import org.veo.persistence.access.DesignatorSequenceRepositoryImpl;
import org.veo.persistence.entity.jpa.VersioningEvent;

import lombok.AllArgsConstructor;

/**
 * Creates designator sequences when a new client is created and removes them
 * when the client is removed.
 */
@Component
@AllArgsConstructor
public class ClientDesignatorInitializer {
    private final DesignatorSequenceRepositoryImpl designatorSequenceRepository;

    @EventListener
    public void handle(VersioningEvent versioningEvent) {
        if (versioningEvent.getEntity() instanceof Client) {
            var client = (Client) versioningEvent.getEntity();
            if (versioningEvent.getType() == VersioningEvent.Type.PERSIST) {
                designatorSequenceRepository.createSequences(client.getId());
            } else if (versioningEvent.getType() == VersioningEvent.Type.REMOVE) {
                designatorSequenceRepository.deleteSequences(client.getId());
            }
        }
    }
}
