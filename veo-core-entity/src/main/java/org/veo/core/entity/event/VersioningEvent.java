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
package org.veo.core.entity.event;

import org.veo.core.entity.Versioned;

import lombok.Getter;

/**
 * This event should be triggered by the persistence layer when a {@link Versioned} is being
 * persisted, updated or removed.
 */
public class VersioningEvent {
  @Getter private final Versioned entity;
  @Getter private final Type type;
  @Getter private final String author;

  public VersioningEvent(Versioned entity, Type type, String author) {
    this.entity = entity;
    this.type = type;
    this.author = author;
  }

  public enum Type {
    PERSIST,
    UPDATE,
    REMOVE
  }
}
