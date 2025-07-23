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

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.veo.core.entity.exception.UnprocessableDataException;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum ElementType {
  @JsonProperty(Asset.SINGULAR_TERM)
  ASSET(EntityType.ASSET),
  @JsonProperty(Control.SINGULAR_TERM)
  CONTROL(EntityType.CONTROL),
  @JsonProperty(Document.SINGULAR_TERM)
  DOCUMENT(EntityType.DOCUMENT),
  @JsonProperty(Incident.SINGULAR_TERM)
  INCIDENT(EntityType.INCIDENT),
  @JsonProperty(Person.SINGULAR_TERM)
  PERSON(EntityType.PERSON),
  @JsonProperty(Process.SINGULAR_TERM)
  PROCESS(EntityType.PROCESS),
  @JsonProperty(Scenario.SINGULAR_TERM)
  SCENARIO(EntityType.SCENARIO),
  @JsonProperty(Scope.SINGULAR_TERM)
  SCOPE(EntityType.SCOPE);
  private final EntityType entityType;

  private static final Map<? extends Class<? extends Entity>, ElementType> TYPE_BY_MODEL_INTERFACE =
      Stream.of(values()).collect(Collectors.toMap(ElementType::getType, it -> it));

  public static final Set<ElementType> RISK_RELATED_ELEMENTS =
      Stream.of(values())
          .filter(type -> RiskRelated.class.isAssignableFrom(type.entityType.getType()))
          .collect(Collectors.toUnmodifiableSet());

  public static final Set<ElementType> RISK_AFFECTED_TYPES =
      Stream.of(values())
          .filter(type -> RiskAffected.class.isAssignableFrom(type.entityType.getType()))
          .collect(Collectors.toUnmodifiableSet());

  // @JsonValue
  public String getSingularTerm() {
    return entityType.getSingularTerm();
  }

  public String getPluralTerm() {
    return entityType.getPluralTerm();
  }

  // @JsonCreator
  public static ElementType fromString(String value) {
    for (ElementType type : ElementType.values()) {
      if (type.name().equalsIgnoreCase(value)) {
        return type;
      }
    }
    throw new UnprocessableDataException("Invalid ElementType: " + value);
  }

  public boolean matches(Element element) {
    return this == element.getType();
  }

  public Class<? extends Element> getType() {
    return (Class<? extends Element>) entityType.getType();
  }

  public Class<Element> getTypeStrict() {
    return (Class<Element>) entityType.getType();
  }

  public static ElementType fromModelInterface(Class<? extends Entity> modelInterface) {
    return Optional.ofNullable(TYPE_BY_MODEL_INTERFACE.get(modelInterface))
        .orElseThrow(
            () -> new IllegalArgumentException("Unsupported element type: " + modelInterface));
  }
}
