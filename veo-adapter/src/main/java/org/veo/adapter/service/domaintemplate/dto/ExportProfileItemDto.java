/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Urs Zeidler
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
package org.veo.adapter.service.domaintemplate.dto;

import java.util.HashSet;
import java.util.Set;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;

import org.veo.adapter.presenter.api.Patterns;
import org.veo.adapter.presenter.api.common.IdRef;
import org.veo.adapter.presenter.api.dto.AbstractCatalogItemDto;
import org.veo.adapter.presenter.api.dto.AbstractProfileTailoringReferenceDto;
import org.veo.adapter.presenter.api.dto.CustomAspectMapDto;
import org.veo.adapter.presenter.api.dto.create.CreateTailoringReferenceDto;
import org.veo.adapter.presenter.api.response.IdentifiableDto;
import org.veo.core.entity.CatalogItem;
import org.veo.core.entity.Identifiable;
import org.veo.core.entity.ProfileItem;
import org.veo.core.entity.aspects.SubTypeAspect;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
public class ExportProfileItemDto extends AbstractCatalogItemDto implements IdentifiableDto {

  @Pattern(regexp = Patterns.UUID, message = "ID must be a valid UUID string following RFC 4122.")
  @Schema(
      description = "ID must be a valid UUID string following RFC 4122.",
      example = "adf037f1-0089-48ad-9177-92269918758b",
      format = "uuid")
  @ToString.Include
  private String id;

  @NotNull
  @Schema(description = "The status for the Element.", example = "NEW")
  @Size(min = 1, max = SubTypeAspect.STATUS_MAX_LENGTH)
  private String status;

  @Valid
  @Schema(
      description = "Groups of customizable attributes - see '/schemas'",
      title = "CustomAspect")
  private CustomAspectMapDto customAspects = new CustomAspectMapDto();

  @JsonTypeInfo(
      use = JsonTypeInfo.Id.NAME,
      visible = true,
      defaultImpl = CreateTailoringReferenceDto.class,
      include = As.EXISTING_PROPERTY,
      property = "referenceType")
  @JsonSubTypes({
    @JsonSubTypes.Type(value = ExportLinkProfileTailoringReference.class, name = "LINK_EXTERNAL"),
    @JsonSubTypes.Type(value = ExportLinkProfileTailoringReference.class, name = "LINK"),
    @JsonSubTypes.Type(value = ExportRiskProfileTailoringReference.class, name = "RISK")
  })
  @Schema(description = "References to other catalog items in the same domain")
  private Set<AbstractProfileTailoringReferenceDto> tailoringReferences = new HashSet<>();

  @Deprecated // TODO #2301 remove
  @ToString.Include
  @Schema(description = "The namespace for the catalogitem.", example = "A1.B2")
  @Size(max = CatalogItem.NAMESPACE_MAX_LENGTH)
  private String namespace;

  private IdRef<CatalogItem> appliedCatalogItem;

  @Override
  public Class<? extends Identifiable> getModelInterface() {
    return ProfileItem.class;
  }
}
