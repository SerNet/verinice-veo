/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2025  Jochen Kemnade.
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
package org.veo.core.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum ElementType {
  ASSET(EntityType.ASSET),
  CONTROL(EntityType.CONTROL),
  DOCUMENT(EntityType.DOCUMENT),
  INCIDENT(EntityType.INCIDENT),
  PERSON(EntityType.PERSON),
  PROCESS(EntityType.PROCESS),
  SCENARIO(EntityType.SCENARIO),
  SCOPE(EntityType.SCOPE);
  private final EntityType entityType;

  @JsonValue
  public String getSingularTerm() {
    return entityType.getSingularTerm();
  }

  @JsonCreator
  public static ElementType fromString(String value) {
    for (ElementType type : ElementType.values()) {
      if (type.name().equalsIgnoreCase(value)) {
        return type;
      }
    }
    throw new IllegalArgumentException("Invalid ElementType: " + value);
  }
}
