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

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.veo.core.VeoConstants;
import org.veo.core.entity.Displayable;
import org.veo.core.entity.Element;
import org.veo.core.entity.Identifiable;
import org.veo.core.entity.Nameable;
import org.veo.core.entity.ref.ITypedId;
import org.veo.core.entity.ref.TypedId;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import lombok.Setter;

@Data
@SuppressWarnings("PMD.ClassWithOnlyPrivateConstructorsShouldBeFinal")
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@JsonFilter(VeoConstants.JSON_FILTER_IDREF)
public class IdRef<T extends Identifiable> implements IIdRef, ITypedId<T> {
  @JsonIgnore private final ITypedId<T> ref;
  private final String displayName;
  @JsonIgnore protected final ReferenceAssembler urlAssembler;

  @JsonIgnore
  @Setter(AccessLevel.NONE)
  private String uri;

  @JsonIgnore private final Identifiable entity;

  /** Create a IdRef for the given entity. */
  public static <T extends Identifiable> IdRef<T> from(
      T entity, @NonNull ReferenceAssembler urlAssembler) {
    if (entity == null) return null;
    return new IdRef<>(
        TypedId.from(entity), ((Displayable) entity).getDisplayName(), urlAssembler, null, entity);
  }

  public static IdRef<?> fromUri(String uri, @NonNull ReferenceAssembler urlAssembler) {
    return new IdRef<>(urlAssembler.parseIdentifiableRef(uri), null, urlAssembler, uri, null);
  }

  @Override
  public String getTargetUri() {
    if (uri == null) {
      uri = urlAssembler.targetReferenceOf(entity);
    }
    return uri;
  }

  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  public UUID getId() {
    return ref.getId();
  }

  @JsonIgnore
  public Class<T> getType() {
    return ref.getType();
  }

  @Override
  public String getSearchesUri() {
    return urlAssembler.searchesReferenceOf(getType());
  }

  @Override
  public String getResourcesUri() {
    return urlAssembler.resourcesReferenceOf(getType());
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
