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

import static org.veo.core.events.MessageCreatorImpl.EVENT_TYPE_CLIENT_CHANGE;
import static org.veo.core.events.MessageCreatorImpl.EVENT_TYPE_ELEMENT_TYPE_DEFINITION_UPDATE;
import static org.veo.rest.VeoRestConfiguration.PROFILE_BACKGROUND_TASKS;

import java.util.Map;
import java.util.UUID;

import org.springframework.amqp.rabbit.annotation.Argument;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.veo.core.entity.EntityType;
import org.veo.core.entity.event.ClientChangedEvent;
import org.veo.core.entity.event.ClientEvent.ClientChangeType;
import org.veo.core.repository.DomainRepository;
import org.veo.core.usecase.IncomingMessageHandler;
import org.veo.message.EventMessage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Subscribes to incoming messages from the external RabbitMQ, parses the messages and forwards them
 * to the {@link IncomingMessageHandler}.
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
  @Autowired ApplicationEventPublisher publisher;

  @RabbitListener(
      bindings =
          @QueueBinding(
              value =
                  @Queue(
                      value = "${veo.message.queues.veo}",
                      exclusive = "false",
                      durable = "true",
                      autoDelete = "${veo.message.consume.autoDelete:false}",
                      arguments =
                          @Argument(
                              name = "x-dead-letter-exchange",
                              value = "${veo.message.consume.dlx}")),
              exchange = @Exchange(value = "${veo.message.exchanges.veo}", type = "topic"),
              key = {
                "${veo.message.routing-key-prefix}" + EVENT_TYPE_ELEMENT_TYPE_DEFINITION_UPDATE
              }))
  public void handleEventMessage(EventMessage event) throws JsonProcessingException {
    log.info("handle message: {} {}", event.getRoutingKey(), event);
    try {
      var content = objectMapper.readTree(event.getContent());
      var eventType = content.get("eventType").asText();
      switch (eventType) {
        case EVENT_TYPE_ELEMENT_TYPE_DEFINITION_UPDATE ->
            handleElementTypeDefinitionUpdate(content);
        default -> throw new IllegalArgumentException("Unexpected event type value: " + eventType);
      }
    } catch (Exception e) {
      log.error("Error while handleEventMessage", e);
      throw e;
    }
  }

  @RabbitListener(
      bindings =
          @QueueBinding(
              value =
                  @Queue(
                      value = "${veo.message.queues.veo-subscriptions}",
                      exclusive = "false",
                      durable = "true",
                      autoDelete = "${veo.message.consume.autoDelete:false}",
                      arguments =
                          @Argument(
                              name = "x-dead-letter-exchange",
                              value = "${veo.message.consume.dlx}")),
              exchange =
                  @Exchange(value = "${veo.message.exchanges.veo-subscriptions}", type = "topic"),
              key = {"${veo.message.routing-key-prefix}" + EVENT_TYPE_CLIENT_CHANGE}))
  public void handleSubscriptionMessage(EventMessage event) throws JsonProcessingException {
    log.info("handle subscription message: {} {}", event.getRoutingKey(), event);
    try {
      var content = objectMapper.readTree(event.getContent());
      var eventType = content.get("eventType").asText();
      switch (eventType) {
        case EVENT_TYPE_CLIENT_CHANGE -> handleClientStateEvent(content);
        default -> throw new IllegalArgumentException("Unexpected event type value: " + eventType);
      }
    } catch (Exception e) {
      log.error("Error while handleEventMessage", e);
      throw e;
    }
  }

  private void handleElementTypeDefinitionUpdate(JsonNode content) {
    var domainId = UUID.fromString(content.get("domainId").asText());
    var elementType = EntityType.getBySingularTerm(content.get("elementType").asText());
    log.info(
        "Received {} message for element type {} in domain {}",
        EVENT_TYPE_ELEMENT_TYPE_DEFINITION_UPDATE,
        elementType,
        domainId);
    domainRepository
        .findById(domainId)
        .ifPresent(
            domain ->
                AsSystemUser.runInClient(
                    domain.getOwner(),
                    () ->
                        incomingMessageHandler.handleElementTypeDefinitionUpdate(
                            domain, elementType)));
  }

  private void handleClientStateEvent(JsonNode content) {
    var clientId = UUID.fromString(content.get("clientId").asText());
    var clientState = ClientChangeType.valueOf(content.get("type").asText());
    var maxUnits = content.has("maxUnits") ? content.get("maxUnits").asInt() : null;
    var clientName = content.has("name") ? content.get("name").asText() : null;
    var domainProducts =
        content.has("domainProducts")
            ? objectMapper.convertValue(content.get("domainProducts"), Map.class)
            : null;
    log.info(
        "Received {} message for clientstate {} message type: {}",
        EVENT_TYPE_CLIENT_CHANGE,
        clientId,
        clientState.name());
    AsSystemUser.runAsAdmin(
        () ->
            publisher.publishEvent(
                new ClientChangedEvent(
                    clientId, clientState, maxUnits, clientName, domainProducts)));
  }
}
