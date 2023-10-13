/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Urs Zeidler.
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
import org.veo.adapter.presenter.api.dto.AbstractTailoringReferenceDto;
import org.veo.adapter.presenter.api.dto.AbstractTemplateItemDto;
import org.veo.adapter.presenter.api.dto.CustomAspectMapDto;
import org.veo.adapter.presenter.api.dto.create.CreateTailoringReferenceDto;
import org.veo.adapter.presenter.api.dto.full.FullTailoringReferenceDto;
import org.veo.adapter.presenter.api.response.IdentifiableDto;
import org.veo.core.entity.CatalogItem;
import org.veo.core.entity.aspects.SubTypeAspect;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/** Complete catalog item including tailoring references & custom aspects */
@EqualsAndHashCode(callSuper = true)
@Data
public class FullCatalogItemDto extends AbstractTemplateItemDto implements IdentifiableDto {

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
    @JsonSubTypes.Type(value = ExportLinkTailoringReference.class, name = "LINK_EXTERNAL"),
    @JsonSubTypes.Type(value = ExportLinkTailoringReference.class, name = "LINK"),
    @JsonSubTypes.Type(value = FullTailoringReferenceDto.class, name = "OMIT"),
    @JsonSubTypes.Type(value = FullTailoringReferenceDto.class, name = "COPY"),
    @JsonSubTypes.Type(value = FullTailoringReferenceDto.class, name = "COPY_ALWAYS"),
    @JsonSubTypes.Type(value = FullTailoringReferenceDto.class, name = "PART"),
    @JsonSubTypes.Type(value = FullTailoringReferenceDto.class, name = "COMPOSITE")
  })
  @Schema(description = "References to other catalog items in the same domain")
  private Set<AbstractTailoringReferenceDto> tailoringReferences = new HashSet<>();

  @Deprecated // TODO #2301 remove
  @ToString.Include
  @Schema(description = "The namespace for the catalogitem.", example = "A1.B2")
  @Size(max = CatalogItem.NAMESPACE_MAX_LENGTH)
  private String namespace;
}
