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
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.veo.core.entity.event.StoredEvent;

public interface StoredEventRepository {
  StoredEvent save(StoredEvent event);

  void remove(StoredEvent event);

  /**
   * Retrieves stored events that are either not locked or have been locked before given point in
   * time, sorted by creation time in descending order.
   *
   * @param maxLockTime Locked events are only included if they've been locked before this point in
   *     time.
   * @param maxResults the maximum number of results to return
   */
  List<StoredEvent> findPendingEvents(Instant maxLockTime, int maxResults);

  Optional<StoredEvent> findById(Long id);

  void delete(Set<Long> ids);

  void saveAll(Collection<StoredEvent> events);
}
