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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.veo.adapter.presenter.api.common.ReferenceAssembler;
import org.veo.adapter.presenter.api.response.transformer.EntityToDtoTransformer;
import org.veo.core.entity.AbstractRisk;
import org.veo.core.entity.ClientOwned;
import org.veo.core.entity.Domain;
import org.veo.core.entity.EntityType;
import org.veo.core.entity.Identifiable;
import org.veo.core.entity.Versioned;
import org.veo.core.entity.event.ClientOwnedEntityVersioningEvent;
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

  public static final String EVENT_TYPE = "eventType";
  public static final String EVENT_TYPE_DOMAIN_CREATION = "domain_creation";
  public static final String EVENT_TYPE_CLIENT_CHANGE = "client_change";
  public static final String EVENT_TYPE_ENTITY_REVISION = "entity_revision";
  public static final String EVENT_TYPE_ELEMENT_TYPE_DEFINITION_UPDATE =
      "element_type_definition_update";

  @Value("${veo.message.routing-key-prefix}")
  private String routingKeyPrefix;

  @Override
  public <T extends Versioned & ClientOwned> void createEntityRevisionMessage(
      ClientOwnedEntityVersioningEvent<T> versioningEvent) {
    var json =
        createEntityRevisionJson(
            versioningEvent.getEntity(),
            versioningEvent.getType(),
            versioningEvent.getAuthor(),
            versioningEvent.getTime());
    storeMessage(
        EVENT_TYPE_ENTITY_REVISION,
        json,
        getUri(versioningEvent.getEntity()),
        getChangeNumber(versioningEvent.getEntity(), versioningEvent.getType()));
  }

  @Override
  public void createDomainCreationMessage(Domain domain) {
    var json = objectMapper.createObjectNode();
    json.put("domainId", domain.getId().uuidValue());
    json.put("clientId", domain.getOwner().getId().uuidValue());
    if (domain.getDomainTemplate() != null) {
      json.put("domainTemplateId", domain.getDomainTemplate().getId().uuidValue());
    }

    // Domain creation messages do not get a changeNumber. These would be duplicates because
    // every domain creation also publishes an EntityRevisionMessage for the domain: "Domain" is
    // also a "Versioned".
    storeMessage(EVENT_TYPE_DOMAIN_CREATION, json, null, null);
  }

  @Override
  public void createElementTypeDefinitionUpdateMessage(Domain domain, EntityType entityType) {
    var json = objectMapper.createObjectNode();
    json.put("domainId", domain.getId().uuidValue());
    json.put("elementType", entityType.getSingularTerm());
    storeMessage(
        EVENT_TYPE_ELEMENT_TYPE_DEFINITION_UPDATE,
        json,
        null, // no resource -> no uri
        null); // no version -> no change number
    // no circus -> no monkeys
  }

  private void storeMessage(String eventType, ObjectNode content, String uri, Long changeNumber) {
    content.put(EVENT_TYPE, eventType);
    storedEventRepository.save(
        StoredEventData.newInstance(
            content.toString(), routingKeyPrefix + eventType, uri, changeNumber));
  }

  private <T extends Versioned & ClientOwned> ObjectNode createEntityRevisionJson(
      T entity, VersioningEvent.ModificationType type, String author, Instant time) {
    var tree = objectMapper.createObjectNode();
    tree.put("uri", getUri(entity));
    tree.put("type", convertType(type));
    tree.put("changeNumber", getChangeNumber(entity, type));
    tree.put("time", time.toString());
    tree.put("author", author);
    tree.put("clientId", entity.getOwningClient().get().getId().uuidValue());
    tree.set("content", objectMapper.valueToTree(entityToDtoTransformer.transform2Dto(entity)));
    return tree;
  }

  private long getChangeNumber(Versioned entity, VersioningEvent.ModificationType type) {
    if (type == VersioningEvent.ModificationType.PERSIST) {
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
      // FIXME VEO-585 instead of checking for risks, we should rather have a compound-id-interface
      //  and check for that
      return referenceAssembler.targetReferenceOf(risk);
    }
    throw new NotImplementedException("Can't build URI for object of type " + entity.getClass());
  }

  private String convertType(VersioningEvent.ModificationType type) {
    return switch (type) {
      case PERSIST -> "CREATION";
      case UPDATE -> "MODIFICATION";
      case REMOVE -> "HARD_DELETION";
    };
  }
}
