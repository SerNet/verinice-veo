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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.veo.adapter.presenter.api.Patterns;
import org.veo.adapter.presenter.api.common.IdRef;
import org.veo.core.entity.TailoringReferenceType;
import org.veo.core.entity.TailoringReferenceTyped;
import org.veo.core.entity.TemplateItem;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
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
    implements TailoringReferenceTyped {
  @Pattern(regexp = Patterns.UUID, message = "ID must be a valid UUID string following RFC 4122.")
  @Schema(
      description = "ID must be a valid UUID string following RFC 4122.",
      example = "adf037f1-0089-48ad-9177-92269918758b",
      format = "uuid")
  @ToString.Include
  private String id;

  private TailoringReferenceType referenceType;

  public IdRef<T> getTarget() {
    return legacyTarget != null ? legacyTarget : target;
  }

  private IdRef<T> target;

  // TODO #2504 remove
  /**
   * Legacy target reference, so old domain template DTOs can still be imported and the {@link
   * org.veo.adapter.presenter.api.dto.full.LegacyCatalogItemDto} stays the same).
   */
  @Deprecated
  @Getter(AccessLevel.PRIVATE)
  @JsonProperty(value = "catalogItem")
  @Schema(
      description =
          "References target catalog item. This legacy property is replaced by the newer \"target\" property. If both properties are defined in a request body, this legacy property takes precedence.",
      deprecated = true)
  private IdRef<T> legacyTarget;
}