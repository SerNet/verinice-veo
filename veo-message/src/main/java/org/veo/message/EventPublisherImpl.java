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
package org.veo.message;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import org.veo.core.entity.event.ControlPartsChangedEvent;
import org.veo.core.entity.event.DomainEvent;
import org.veo.core.entity.event.RiskEvent;
import org.veo.core.entity.event.StoredEvent;
import org.veo.core.service.EventPublisher;

/** Implementation of a domain event publisher using Spring's {@code ApplicationEventPublisher}. */
@Service
public class EventPublisherImpl implements EventPublisher {

  @Autowired private ApplicationEventPublisher publisher;

  @Override
  public void publish(StoredEvent event) {
    publisher.publishEvent(event);
  }

  @Override
  public void publish(RiskEvent event) {
    publisher.publishEvent(event);
  }

  @Override
  public void publish(ControlPartsChangedEvent event) {
    publisher.publishEvent(event);
  }

  @Override
  public void publish(DomainEvent event) {
    publisher.publishEvent(event);
  }
}
