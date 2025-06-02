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

import org.veo.core.entity.CompoundIdentifiable;
import org.veo.core.entity.Designated;
import org.veo.core.entity.Displayable;
import org.veo.core.entity.Identifiable;
import org.veo.core.entity.Nameable;
import org.veo.core.entity.ref.ITypedCompoundId;
import org.veo.core.entity.ref.TypedCompoundId;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import lombok.Setter;

@Data
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class CompoundIdRef<
        T extends CompoundIdentifiable<TFirst, TSecond>,
        TFirst extends Identifiable,
        TSecond extends Identifiable>
    implements ITypedCompoundId<T, TFirst, TSecond>, IIdRef {

  @JsonIgnore private final ITypedCompoundId<T, TFirst, TSecond> ref;

  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  @Override
  public UUID getFirstId() {
    return ref.getFirstId();
  }

  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  @Override
  public UUID getSecondId() {
    return ref.getSecondId();
  }

  @JsonIgnore
  @Override
  public Class<T> getType() {
    return ref.getType();
  }

  @JsonIgnore
  @Override
  public Class<TFirst> getFirstType() {
    return ref.getFirstType();
  }

  @JsonIgnore
  @Override
  public Class<TSecond> getSecondType() {
    return ref.getSecondType();
  }

  @JsonIgnore protected final ReferenceAssembler urlAssembler;

  @JsonIgnore
  @Setter(AccessLevel.NONE)
  private String uri;

  @JsonIgnore private final T entity;

  /** Create a IdRef for the given entity. */
  public static <
          T extends CompoundIdentifiable<TFirst, TSecond>,
          TFirst extends Identifiable,
          TSecond extends Identifiable>
      CompoundIdRef<T, TFirst, TSecond> from(T entity, @NonNull ReferenceAssembler urlAssembler) {
    if (entity == null) return null;
    return new CompoundIdRef<>(TypedCompoundId.from(entity), urlAssembler, null, entity);
  }

  @Override
  public String getTargetUri() {
    if (uri == null) {
      uri = urlAssembler.targetReferenceOf(entity);
    }
    return uri;
  }

  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  public String getDesignator() {
    if (entity instanceof Designated designated) {
      return designated.getDesignator();
    }
    return null;
  }

  @Override
  public String getDisplayName() {
    if (entity instanceof Displayable d) {
      return d.getDisplayName();
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
