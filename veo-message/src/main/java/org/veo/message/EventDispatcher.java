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

import static java.lang.Long.parseLong;
import static java.util.Objects.requireNonNull;

import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class EventDispatcher {

  private final RabbitTemplate rabbitTemplate;

  private final ConcurrentLinkedQueue<Consumer<Long>> ackCallbacks = new ConcurrentLinkedQueue<>();

  @Autowired
  EventDispatcher(RabbitTemplate rabbitTemplate) {
    this.rabbitTemplate = rabbitTemplate;
    rabbitTemplate.setConfirmCallback(
        ((correlationData, ack, cause) -> {
          requireNonNull(correlationData);
          var messageId = parseLong(correlationData.getId());
          var returnedMessage = correlationData.getReturned();
          if (returnedMessage != null) {
            log.warn(
                "Message for event {} returned with code {}: {}",
                messageId,
                returnedMessage.getReplyCode(),
                returnedMessage.getMessage());
          } else if (!ack) {
            log.warn("message with id {} was not acked", messageId);
          } else {
            log.debug("message with id {} was acked", messageId);
            ackCallbacks.forEach(cb -> cb.accept(messageId));
          }
        }));
  }

  public void send(String exchange, EventMessage event) {
    log.debug(
        "Sending event id: {}, timestamp: {}, routing-key: {}",
        event.getId(),
        event.getTimestamp(),
        event.getRoutingKey());
    rabbitTemplate.convertAndSend(
        exchange, event.getRoutingKey(), event, new CorrelationData(event.getId().toString()));
  }

  public void send(String exchange, Set<EventMessage> events) {
    events.forEach(event -> send(exchange, event));
  }

  public void addAckCallback(Consumer<Long> ackCallback) {
    ackCallbacks.add(ackCallback);
  }
}
