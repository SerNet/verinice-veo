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
package org.veo.persistence.access;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.veo.core.entity.event.StoredEvent;

public interface StoredEventRepository {
    StoredEvent save(StoredEvent event);

    Set<StoredEvent> findAll();

    void remove(StoredEvent event);

    /**
     * Retrieves unprocessed stored events, oldest first.
     *
     * @param maxLockTime
     *            Locked events are only included if they've been locked before this
     *            point in time.
     */
    List<StoredEvent> findPendingEvents(Instant maxLockTime);

    List<StoredEvent> saveAll(List<StoredEvent> pendingEvents);
}
