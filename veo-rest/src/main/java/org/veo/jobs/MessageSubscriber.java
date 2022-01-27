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
package org.veo.jobs;

import static org.veo.core.events.MessageCreatorImpl.ROUTING_KEY_ELEMENT_TYPE_DEFINITION_UPDATE;
import static org.veo.rest.VeoRestConfiguration.PROFILE_BACKGROUND_TASKS;

import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.veo.core.entity.EntityType;
import org.veo.core.entity.Key;
import org.veo.core.repository.DomainRepository;
import org.veo.core.usecase.IncomingMessageHandler;
import org.veo.message.EventMessage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Subscribes to incoming messages from the external RabbitMQ, parses the
 * messages and forwards them to the {@link IncomingMessageHandler}.
 */
@Component
@Profile(PROFILE_BACKGROUND_TASKS)
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MessageSubscriber {
    private final ObjectMapper objectMapper;
    private final DomainRepository domainRepository;
    private final IncomingMessageHandler incomingMessageHandler;

    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = "${veo.message.consume.queue}",
                                                            exclusive = "false",
                                                            durable = "true",
                                                            autoDelete = "false"),
                                             exchange = @Exchange(value = "${veo.message.dispatch.exchange}",
                                                                  type = "topic"),
                                             key = "${veo.message.dispatch.routing-key-prefix}"
                                                     + ROUTING_KEY_ELEMENT_TYPE_DEFINITION_UPDATE))
    public void handleElementTypeDefinitionUpdate(EventMessage event)
            throws JsonProcessingException {
        var content = objectMapper.readTree(event.getContent());
        var domainId = Key.uuidFrom(content.get("domainId")
                                           .asText());
        var elementType = EntityType.getBySingularTerm(content.get("elementType")
                                                              .asText());
        log.info("Received {} message for element type {} in domain {}",
                 ROUTING_KEY_ELEMENT_TYPE_DEFINITION_UPDATE, elementType, domainId.uuidValue());
        domainRepository.findById(domainId)
                        .ifPresent(domain -> {
                            AsSystemUser.runInClient(domain.getOwner(), () -> {
                                incomingMessageHandler.handleElementTypeDefinitionUpdate(domain,
                                                                                         elementType);
                            });
                        });
    }
}
