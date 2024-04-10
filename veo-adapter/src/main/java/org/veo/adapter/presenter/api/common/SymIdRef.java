/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2024  Jonas Jordan
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
import org.veo.core.entity.Identifiable;
import org.veo.core.entity.Nameable;
import org.veo.core.entity.SymIdentifiable;
import org.veo.core.entity.ref.ITypedSymbolicId;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Setter;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@SuppressWarnings("PMD.ClassWithOnlyPrivateConstructorsShouldBeFinal")
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class SymIdRef<T extends SymIdentifiable<T, TNamespace>, TNamespace extends Identifiable>
    implements IIdRef, ITypedSymbolicId<T, TNamespace> {

  @JsonProperty(value = "id", access = JsonProperty.Access.READ_ONLY)
  @EqualsAndHashCode.Include
  private final String symbolicId;

  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  @EqualsAndHashCode.Include
  private final String namespaceId;

  @Schema(accessMode = Schema.AccessMode.READ_ONLY)
  private final String displayName;

  @JsonIgnore @EqualsAndHashCode.Include private final Class<T> type;
  @JsonIgnore @EqualsAndHashCode.Include private final Class<TNamespace> namespaceType;

  @JsonIgnore protected final ReferenceAssembler urlAssembler;

  @JsonIgnore
  @Setter(AccessLevel.NONE)
  private String uri;

  @JsonIgnore private final T entity;

  private SymIdRef(
      T entity,
      String id,
      String namespaceId,
      String displayName,
      Class<T> type,
      Class<TNamespace> namespaceType,
      ReferenceAssembler referenceAssembler) {
    this(id, namespaceId, displayName, type, namespaceType, referenceAssembler, null, entity);
  }

  private SymIdRef(
      String uri,
      String id,
      String namespaceId,
      Class<T> type,
      Class<TNamespace> namespaceType,
      ReferenceAssembler referenceAssembler) {
    this(id, namespaceId, null, type, namespaceType, referenceAssembler, uri, null);
  }

  /** Create an IdRef for the given entity. */
  public static <T extends SymIdentifiable<T, TNamespace>, TNamespace extends Identifiable>
      SymIdRef<T, TNamespace> from(T entity, @NonNull ReferenceAssembler urlAssembler) {
    if (entity == null) return null;
    return new SymIdRef<>(
        entity,
        entity.getSymbolicIdAsString(),
        entity.getNamespace().getIdAsString(),
        ((Displayable) entity).getDisplayName(),
        (Class<T>) entity.getModelInterface(),
        (Class<TNamespace>) entity.getNamespace().getModelInterface(),
        urlAssembler);
  }

  public static <T extends SymIdentifiable<T, TNamespace>, TNamespace extends Identifiable>
      SymIdRef<T, TNamespace> fromUri(String uri, @NonNull ReferenceAssembler urlAssembler) {
    return new SymIdRef<>(
        uri,
        urlAssembler.parseId(uri),
        urlAssembler.parseNamespaceId(uri),
        (Class<T>) urlAssembler.parseType(uri),
        (Class<TNamespace>) urlAssembler.parseNamespaceType(uri),
        urlAssembler);
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
    return null;
  }

  @Override
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  public String getResourcesUri() {
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

  @Override
  public boolean equals(Object other) {
    return ITypedSymbolicId.equals(this, other);
  }

  @Override
  public int hashCode() {
    return ITypedSymbolicId.hashCode(this);
  }

  @Override
  public String toString() {
    return ITypedSymbolicId.toString(this);
  }
}
