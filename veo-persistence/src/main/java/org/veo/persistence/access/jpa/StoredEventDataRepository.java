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
package org.veo.persistence.access.jpa;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import org.veo.core.entity.event.StoredEvent;
import org.veo.persistence.entity.jpa.StoredEventData;

@Transactional(readOnly = true)
public interface StoredEventDataRepository extends JpaRepository<StoredEventData, Long> {
  @Query(
      "select e from #{#entityName} as e "
          + "where e.processed = false and (e.lockTime is null or e.lockTime < ?1) order by e.id")
  List<StoredEvent> findPendingEvents(Instant maxLockTime);
}
