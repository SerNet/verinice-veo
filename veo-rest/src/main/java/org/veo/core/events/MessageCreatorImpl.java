/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Jonas Jordan
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
package org.veo.core.events;

import java.time.Instant;

import org.apache.commons.lang3.NotImplementedException;
import org.springframework.beans.factory.annotation.Value;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.veo.adapter.presenter.api.common.ReferenceAssembler;
import org.veo.adapter.presenter.api.response.transformer.EntityToDtoTransformer;
import org.veo.core.entity.AbstractRisk;
import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.core.entity.EntityType;
import org.veo.core.entity.Identifiable;
import org.veo.core.entity.Versioned;
import org.veo.core.entity.event.VersioningEvent;
import org.veo.core.usecase.MessageCreator;
import org.veo.persistence.access.StoredEventRepository;
import org.veo.persistence.entity.jpa.StoredEventData;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @see MessageCreator
 */
@RequiredArgsConstructor
@Slf4j
public class MessageCreatorImpl implements MessageCreator {
  private final StoredEventRepository storedEventRepository;
  private final ObjectMapper objectMapper;
  private final ReferenceAssembler referenceAssembler;
  private final EntityToDtoTransformer entityToDtoTransformer;

  // TODO VEO-1084 rename routing keys
  public static final String ROUTING_KEY_DOMAIN_CREATION = "domain_creation_event";
  public static final String ROUTING_KEY_ELEMENT_TYPE_DEFINITION_UPDATE =
      "element_type_definition_update";
  public static final String ROUTING_KEY_ELEMENT_CLIENT_CHANGE = "client_change";

  public static final String ROUTING_KEY_ENTITY_REVISION = "versioning_event";

  @Value("${veo.message.dispatch.routing-key-prefix}")
  private String routingKeyPrefix;

  @Override
  public void createEntityRevisionMessage(VersioningEvent versioningEvent, Client client) {
    var json =
        createEntityRevisionJson(
            versioningEvent.getEntity(),
            versioningEvent.getType(),
            versioningEvent.getAuthor(),
            versioningEvent.getTime(),
            client);
    storeMessage(ROUTING_KEY_ENTITY_REVISION, json);
  }

  @Override
  public void createDomainCreationMessage(Domain domain) {
    var json = objectMapper.createObjectNode();
    json.put("domainId", domain.getId().uuidValue());
    json.put("clientId", domain.getOwner().getId().uuidValue());
    if (domain.getDomainTemplate() != null) {
      json.put("domainTemplateId", domain.getDomainTemplate().getId().uuidValue());
    }

    storeMessage(ROUTING_KEY_DOMAIN_CREATION, json);
  }

  @Override
  public void createElementTypeDefinitionUpdateMessage(Domain domain, EntityType entityType) {
    var json = objectMapper.createObjectNode();
    json.put("domainId", domain.getId().uuidValue());
    json.put("elementType", entityType.getSingularTerm());
    storeMessage(ROUTING_KEY_ELEMENT_TYPE_DEFINITION_UPDATE, json);
  }

  private void storeMessage(String routingKey, JsonNode content) {
    storedEventRepository.save(
        StoredEventData.newInstance(content.toString(), routingKeyPrefix + routingKey));
  }

  private JsonNode createEntityRevisionJson(
      Versioned entity, VersioningEvent.Type type, String author, Instant time, Client client) {
    var tree = objectMapper.createObjectNode();
    tree.put("uri", getUri(entity));
    tree.put("type", convertType(type));
    tree.put("changeNumber", getChangeNumber(entity, type));
    tree.put("time", time.toString());
    tree.put("author", author);
    tree.put("clientId", client.getId().uuidValue());
    tree.set("content", objectMapper.valueToTree(entityToDtoTransformer.transform2Dto(entity)));
    return tree;
  }

  private long getChangeNumber(Versioned entity, VersioningEvent.Type type) {
    if (type == VersioningEvent.Type.PERSIST) {
      return 0;
    }
    // We use the JPA version number as a base for our continuous change number.
    // When updating an entity, JPA increments the version number after this message
    // creation, so we must add 1 to the version number. We must also add 1 in case
    // of a deletion, because JPA won't assign a new number for a deleted entity.
    return entity.getVersion() + 1;
  }

  private String getUri(Versioned entity) {
    if (entity instanceof Identifiable identifiable) {
      return referenceAssembler.targetReferenceOf(identifiable);
    }
    if (entity instanceof AbstractRisk<?, ?> risk) {
      return referenceAssembler.targetReferenceOf(risk);
    }
    throw new NotImplementedException("Can't build URI for object of type " + entity.getClass());
  }

  private String convertType(VersioningEvent.Type type) {
    switch (type) {
      case PERSIST:
        return "CREATION";
      case UPDATE:
        return "MODIFICATION";
      case REMOVE:
        return "HARD_DELETION";
    }
    throw new NotImplementedException("Event type " + type + " not supported.");
  }
}
