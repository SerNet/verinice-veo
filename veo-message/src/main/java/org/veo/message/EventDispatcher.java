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

import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class EventDispatcher {

    private final RabbitTemplate rabbitTemplate;

    private final String exchange;

    public static final ConfirmCallback NOP_CALLBACK = (ack) -> {
        /* NOP */};

    @Autowired
    EventDispatcher(RabbitTemplate rabbitTemplate,
            @Value("${veo.message.dispatch.exchange}") String exchange) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
    }

    private void send(EventMessage event, ConfirmCallback callback) {
        log.debug("Sending event id: {}, timestamp: {}, routing-key: {}", event.getId(),
                  event.getTimestamp(), event.getRoutingKey());
        var correlationData = new CorrelationData(event.getId()
                                                       .toString());
        rabbitTemplate.convertAndSend(exchange, event.getRoutingKey(), event, correlationData);
        try {
            var confirm = correlationData.getFuture()
                                         .get(1000, TimeUnit.MILLISECONDS);
            callback.confirm(confirm.isAck());
        } catch (InterruptedException e) {
            log.error("Thread was interrupted while waiting for message confirmation", e);
        } catch (ExecutionException e) {
            log.error("Message confirmation was aborted.", e);
        } catch (TimeoutException e) {
            log.warn("Timed out waiting for confirmation of message with id {}.", event.getId());
        }
    }

    @Async
    public void sendAsync(EventMessage event, ConfirmCallback callback) {
        this.sendAsync(Set.of(event), callback);
    }

    @Async
    public void sendAsync(Set<EventMessage> events, ConfirmCallback callback) {
        events.forEach((e -> this.send(e, callback)));
    }
}
