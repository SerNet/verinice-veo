/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Urs Zeidler.
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

import jakarta.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.veo.adapter.presenter.api.Patterns;
import org.veo.adapter.presenter.api.common.IdRef;
import org.veo.core.VeoConstants;
import org.veo.core.entity.TailoringReferenceType;
import org.veo.core.entity.TemplateItem;
import org.veo.core.entity.ref.ITypedId;
import org.veo.core.entity.state.TailoringReferenceState;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Represents a {@link org.veo.core.entity.TailoringReference}. This base class can be used for many
 * {@link TailoringReferenceType}s, but some types require specific DTO subclasses that can hold
 * additional information.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@JsonIgnoreProperties("linkTailoringReferences")
public class TailoringReferenceDto<T extends TemplateItem<T>> extends AbstractVersionedDto
    implements TailoringReferenceState<T> {
  @Pattern(regexp = Patterns.UUID, message = VeoConstants.UUID_MESSAGE)
  @Schema(
      description = VeoConstants.UUID_MESSAGE,
      example = "adf037f1-0089-48ad-9177-92269918758b",
      format = "uuid")
  @ToString.Include
  private String id;

  private TailoringReferenceType referenceType;

  private IdRef<T> target;

  @Override
  @JsonIgnore
  public ITypedId<T> getTargetRef() {
    return target;
  }
}
