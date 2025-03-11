/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Jonas Jordan
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
package org.veo.core.usecase;

import org.veo.core.entity.ClientOwned;
import org.veo.core.entity.Domain;
import org.veo.core.entity.EntityType;
import org.veo.core.entity.Versioned;
import org.veo.core.entity.event.ClientOwnedEntityVersioningEvent;

/**
 * Creates outgoing messages and persists them so they can be sent to the message queue by a
 * background task.
 */
public interface MessageCreator {
  <T extends Versioned & ClientOwned> void createEntityRevisionMessage(
      ClientOwnedEntityVersioningEvent<T> event);

  void createDomainCreationMessage(Domain domain);

  void createElementTypeDefinitionUpdateMessage(Domain domain, EntityType entityType);

  void createDomainTemplateCreationEvent(Domain sourceDomain);
}
