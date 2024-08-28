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

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.veo.core.entity.Displayable;
import org.veo.core.entity.Identifiable;
import org.veo.core.entity.Nameable;
import org.veo.core.entity.SymIdentifiable;
import org.veo.core.entity.ref.ITypedId;
import org.veo.core.entity.ref.ITypedSymbolicId;
import org.veo.core.entity.ref.TypedSymbolicId;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import lombok.Setter;

@Data
@SuppressWarnings("PMD.ClassWithOnlyPrivateConstructorsShouldBeFinal")
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class SymIdRef<T extends SymIdentifiable<T, TNamespace>, TNamespace extends Identifiable>
    implements IIdRef, ITypedSymbolicId<T, TNamespace> {
  @JsonIgnore private final ITypedSymbolicId<T, TNamespace> ref;
  private final String displayName;
  @JsonIgnore protected final ReferenceAssembler urlAssembler;

  @JsonIgnore
  @Setter(AccessLevel.NONE)
  private String uri;

  @JsonIgnore private final T entity;

  private SymIdRef(
      ITypedSymbolicId<T, TNamespace> ref,
      T entity,
      String displayName,
      ReferenceAssembler referenceAssembler) {
    this(ref, displayName, referenceAssembler, null, entity);
  }

  private SymIdRef(
      String uri, ITypedSymbolicId<T, TNamespace> ref, ReferenceAssembler referenceAssembler) {
    this(ref, null, referenceAssembler, uri, null);
  }

  /** Create an IdRef for the given entity. */
  public static <T extends SymIdentifiable<T, TNamespace>, TNamespace extends Identifiable>
      SymIdRef<T, TNamespace> from(T entity, @NonNull ReferenceAssembler urlAssembler) {
    if (entity == null) return null;
    return new SymIdRef<>(
        TypedSymbolicId.from(entity),
        entity,
        ((Displayable) entity).getDisplayName(),
        urlAssembler);
  }

  public static SymIdRef<?, ?> fromUri(String uri, @NonNull ReferenceAssembler urlAssembler) {
    return new SymIdRef<>(uri, urlAssembler.parseSymIdentifiableUri(uri), urlAssembler);
  }

  @Override
  @JsonProperty(value = "id", access = JsonProperty.Access.READ_ONLY)
  public UUID getSymbolicId() {
    return ref.getSymbolicId();
  }

  @Override
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  public UUID getNamespaceId() {
    return ref.getNamespaceId();
  }

  @Override
  @JsonIgnore
  public ITypedId<TNamespace> getNamespaceRef() {
    return ITypedSymbolicId.super.getNamespaceRef();
  }

  @Override
  @JsonIgnore
  public Class<TNamespace> getNamespaceType() {
    return ref.getNamespaceType();
  }

  @Override
  @JsonIgnore
  public Class<T> getType() {
    return ref.getType();
  }

  @Override
  public String getTargetUri() {
    if (uri == null) {
      uri = urlAssembler.targetReferenceOf(entity);
    }
    return uri;
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

  @JsonProperty(value = "type", access = JsonProperty.Access.READ_ONLY)
  public String getModelType() {
    return entity.getModelType();
  }

  @Override
  public boolean equals(Object other) {
    return ref.equals(other);
  }

  @Override
  public int hashCode() {
    return ref.hashCode();
  }

  @Override
  public String toString() {
    return ref.toString();
  }
}
