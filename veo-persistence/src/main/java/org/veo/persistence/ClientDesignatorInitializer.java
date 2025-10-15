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

import static org.veo.core.entity.event.VersioningEvent.ModificationType.PERSIST;
import static org.veo.core.entity.event.VersioningEvent.ModificationType.REMOVE;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import org.veo.core.entity.event.ClientVersioningEvent;
import org.veo.persistence.access.DesignatorSequenceRepositoryImpl;

import lombok.AllArgsConstructor;

/**
 * Creates designator sequences when a new client is created and removes them when the client is
 * removed.
 */
@Component
@AllArgsConstructor
public class ClientDesignatorInitializer {
  private final DesignatorSequenceRepositoryImpl designatorSequenceRepository;

  @EventListener
  public void handle(ClientVersioningEvent event) {
    if (event.type() == PERSIST) {
      designatorSequenceRepository.createSequences(event.getClientId());
    } else if (event.type() == REMOVE) {
      designatorSequenceRepository.deleteSequences(event.getClientId());
    }
  }
}
