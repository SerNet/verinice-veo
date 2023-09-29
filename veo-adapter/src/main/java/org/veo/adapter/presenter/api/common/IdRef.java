/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Jochen Kemnade.
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
package org.veo.adapter.presenter.api.common;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.veo.core.entity.Displayable;
import org.veo.core.entity.Element;
import org.veo.core.entity.Identifiable;
import org.veo.core.entity.Nameable;
import org.veo.core.entity.ref.ITypedId;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@SuppressWarnings("PMD.ClassWithOnlyPrivateConstructorsShouldBeFinal")
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class IdRef<T extends Identifiable> implements IIdRef, ITypedId<T> {

  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  @ToString.Include
  @EqualsAndHashCode.Include
  private final String id;

  @ToString.Include
  @Schema(accessMode = Schema.AccessMode.READ_ONLY)
  private final String displayName;

  @JsonIgnore @EqualsAndHashCode.Include private final Class<T> type;

  @JsonIgnore protected final ReferenceAssembler urlAssembler;

  @JsonIgnore
  @Setter(AccessLevel.NONE)
  private String uri;

  @JsonIgnore private final Identifiable entity;

  private IdRef(
      Identifiable entity,
      String id,
      String displayName,
      Class<T> type,
      ReferenceAssembler referenceAssembler) {
    this(id, displayName, type, referenceAssembler, null, entity);
  }

  private IdRef(String uri, String id, Class<T> type, ReferenceAssembler referenceAssembler) {
    this(id, null, type, referenceAssembler, uri, null);
  }

  /** Create a IdRef for the given entity. */
  public static <T extends Identifiable> IdRef<T> from(
      T entity, @NonNull ReferenceAssembler urlAssembler) {
    if (entity == null) return null;
    return new IdRef<>(
        entity,
        entity.getId().uuidValue(),
        ((Displayable) entity).getDisplayName(),
        (Class<T>) entity.getModelInterface(),
        urlAssembler);
  }

  public static <T extends Identifiable> IdRef<T> fromUri(
      String uri, @NonNull ReferenceAssembler urlAssembler) {
    return new IdRef<>(
        uri, urlAssembler.parseId(uri), (Class<T>) urlAssembler.parseType(uri), urlAssembler);
  }

  @Override
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  public String getTargetUri() {
    if (uri == null) {
      uri = urlAssembler.targetReferenceOf(entity);
    }
    return uri;
  }

  @Override
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  public String getSearchesUri() {
    return urlAssembler.searchesReferenceOf(type);
  }

  @Override
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  public String getResourcesUri() {
    return urlAssembler.resourcesReferenceOf(type);
  }

  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  public String getDesignator() {
    if (entity instanceof Element designated) {
      return designated.getDesignator();
    }
    return null;
  }

  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  public String getName() {
    if (entity instanceof Nameable nameable) {
      return nameable.getName();
    }
    return null;
  }

  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  public String getAbbreviation() {
    if (entity instanceof Nameable nameable) {
      return nameable.getAbbreviation();
    }
    return null;
  }
}
