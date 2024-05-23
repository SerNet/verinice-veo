/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Alexander Koderman.
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
package org.veo.core.service;

import org.veo.core.entity.event.ControlPartsChangedEvent;
import org.veo.core.entity.event.DomainEvent;
import org.veo.core.entity.event.RiskEvent;
import org.veo.core.entity.event.StoredEvent;

/**
 * Provides a mechanism for domain events to be published. Processes events from use cases and
 * aggregate roots.
 */
public interface EventPublisher {
  void publish(StoredEvent event);

  void publish(RiskEvent event);

  void publish(ControlPartsChangedEvent evt);

  void publish(DomainEvent event);
}
