/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Jochen Kemnade.
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
package org.veo.adapter.presenter.api.dto;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

import org.veo.adapter.presenter.api.common.Ref;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * Implements all members of {@link org.veo.core.entity.Versioned} and has a self reference.
 *
 * <p>TODO VEO-902 rename back to AbstractVersionedDto when all Versioned types can have a self
 * reference.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(makeFinal = true)
public abstract class AbstractVersionedSelfReferencingDto extends AbstractVersionedDto {
  @JsonIgnore
  @Getter(AccessLevel.NONE)
  private Ref selfRef;

  @JsonProperty(value = "_self", access = Access.READ_ONLY)
  @Schema(description = "A valid reference to this resource.", format = "uri")
  public String getSelf() {
    return Optional.ofNullable(selfRef).map(Ref::getTargetUri).orElse(null);
  }
}
