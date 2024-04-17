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

import org.veo.core.entity.CompoundIdentifiable;
import org.veo.core.entity.Designated;
import org.veo.core.entity.Displayable;
import org.veo.core.entity.Identifiable;
import org.veo.core.entity.Nameable;
import org.veo.core.entity.ref.ITypedCompoundId;

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
public class CompoundIdRef<
        T extends CompoundIdentifiable<TFirst, TSecond>,
        TFirst extends Identifiable,
        TSecond extends Identifiable>
    implements ITypedCompoundId<T, TFirst, TSecond>, IIdRef {

  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  @ToString.Include
  @EqualsAndHashCode.Include
  private final String firstId;

  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  @ToString.Include
  @EqualsAndHashCode.Include
  private final String secondId;

  @JsonIgnore @EqualsAndHashCode.Include private final Class<T> type;
  @JsonIgnore @EqualsAndHashCode.Include private final Class<TFirst> firstType;
  @JsonIgnore @EqualsAndHashCode.Include private final Class<TSecond> secondType;

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
    return new CompoundIdRef<>(
        entity.getFirstIdAsString(),
        entity.getSecondIdAsString(),
        (Class<T>) entity.getModelInterface(),
        (Class<TFirst>) entity.getFirstRelation().getModelInterface(),
        (Class<TSecond>) entity.getSecondRelation().getModelInterface(),
        urlAssembler,
        null,
        entity);
  }

  @Override
  public String getTargetUri() {
    if (uri == null) {
      uri = urlAssembler.targetReferenceOf(entity);
    }
    return uri;
  }

  @Override
  public String getSearchesUri() {
    return null;
  }

  @Override
  public String getResourcesUri() {
    return urlAssembler.resourcesReferenceOf(entity);
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
    return ITypedCompoundId.equals(this, other);
  }

  @Override
  public int hashCode() {
    return ITypedCompoundId.hashCode(this);
  }

  @Override
  public String toString() {
    return ITypedCompoundId.toString(this);
  }
}
