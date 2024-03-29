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
package org.veo.core.entity.event;

import java.time.Instant;

/** An event to be forwarded to an external message queue. */
public interface StoredEvent {

  /**
   * @return Sequential identifier of this event.
   */
  Long getId();

  /**
   * @return Message payload.
   */
  String getContent();

  /**
   * @return The timestamp of this event
   */
  Instant getTimestamp();

  /**
   * @return Message queue routing key.
   */
  String getRoutingKey();

  /**
   * Lock this event for processing so other workers don't process it redundantly. This sets the
   * lock time to now.
   */
  void lock();

  /**
   * @return Moment when a worker locked this event for processing or null if it isn't locked. Locks
   *     on an event prevent multiple workers from processing the same event redundantly.
   *     Unprocessed events that have been locked a long time ago are probably stuck and should be
   *     locked and processed by another worker.
   */
  Instant getLockTime();
}
