/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2024  Urs Zeidler
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
package org.veo.core.entity.event;

import java.util.UUID;

import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** An event that is triggered for the target entity when a link is created, updated or deleted. */
@EqualsAndHashCode
@RequiredArgsConstructor
public class InboundLinkEvent implements ElementEvent {
  private final Element entity;
  private final Domain domain;
  @Getter private final String linkType;
  @Getter private final Operation operation;
  @Getter private final Object source;

  @Override
  public UUID getEntityId() {
    return entity.getId();
  }

  @SuppressWarnings("unchecked")
  @Override
  public Class<? extends Element> getEntityType() {
    return (Class<? extends Element>) entity.getModelInterface();
  }

  @Override
  public UUID getClientId() {
    return entity.getOwningClient().get().getId();
  }

  @Override
  public UUID getDomainId() {
    return domain.getId();
  }

  public enum Operation {
    CREATION_OR_UPDATE,
    DELETION
  }
}
