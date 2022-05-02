/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Jochen Kemnade.
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

import org.veo.core.entity.Element;
import org.veo.core.entity.Key;

import lombok.RequiredArgsConstructor;

/** An event that is triggered when a risk-relevant change it made to an entity */
@RequiredArgsConstructor
public class RiskComponentChangeEvent {

  /**
   * This allows events to be published for entities that have not yet been persisted and whose ID
   * is therefore {@code null}. The ID will have been initialized however when the event listener
   * accesses it. NOTE: this requires that event listeners run only after successful commit. This is
   * the default for {@code TransactionalEventListener}.
   */
  private final Element entity;

  public Key<UUID> getEntityId() {
    return entity.getId();
  }

  @SuppressWarnings("unchecked")
  public Class<? extends Element> getEntityType() {
    return (Class<? extends Element>) entity.getModelInterface();
  }
}
