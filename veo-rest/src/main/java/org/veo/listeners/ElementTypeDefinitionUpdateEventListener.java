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
package org.veo.listeners;

import static org.springframework.transaction.annotation.Propagation.MANDATORY;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import org.veo.core.entity.event.ElementTypeDefinitionUpdateEvent;
import org.veo.core.usecase.MessageCreator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Listens to {@link ElementTypeDefinitionUpdateEvent}s from the persistence layer and forwards them
 * to the {@link MessageCreator}.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ElementTypeDefinitionUpdateEventListener {
  private final MessageCreator messageCreator;

  @EventListener
  @Transactional(propagation = MANDATORY)
  void handle(ElementTypeDefinitionUpdateEvent event) {
    messageCreator.createElementTypeDefinitionUpdateMessage(
        event.getDomain(), event.getElementTypeDefinition().getElementType());
  }
}
