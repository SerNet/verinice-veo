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

import org.veo.adapter.presenter.api.Patterns;
import org.veo.adapter.presenter.api.common.ReferenceAssembler;
import org.veo.adapter.presenter.api.dto.AbstractTemplateItemDto;
import org.veo.adapter.presenter.api.dto.CustomAspectMapDto;
import org.veo.adapter.presenter.api.dto.TailoringReferenceDto;
import org.veo.adapter.presenter.api.response.IdentifiableDto;
import org.veo.core.VeoConstants;
import org.veo.core.entity.CatalogItem;
import org.veo.core.entity.TailoringReference;
import org.veo.core.entity.TemplateItemAspects;
import org.veo.core.entity.aspects.SubTypeAspect;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;

/** Complete catalog item including tailoring references & custom aspects */
@EqualsAndHashCode(callSuper = true)
@Data
public class FullCatalogItemDto extends AbstractTemplateItemDto<CatalogItem>
    implements IdentifiableDto, FullTemplateItemDto<CatalogItem> {

  @Pattern(regexp = Patterns.UUID, message = VeoConstants.UUID_MESSAGE)
  @Schema(
      description = VeoConstants.UUID_MESSAGE,
      example = "adf037f1-0089-48ad-9177-92269918758b",
      format = "uuid")
  @ToString.Include
  private String id;

  @NotNull
  @Schema(description = "The status for the Element.", example = "NEW")
  @Size(min = 1, max = SubTypeAspect.STATUS_MAX_LENGTH)
  private String status;

  @Valid @NotNull private TemplateItemAspects aspects = new TemplateItemAspects();

  @Valid
  @Schema(
      description = "Groups of customizable attributes - see '/schemas'",
      title = "CustomAspect")
  private CustomAspectMapDto customAspects = new CustomAspectMapDto();

  private Set<TailoringReferenceDto<CatalogItem>> tailoringReferences = new HashSet<>();

  public void add(
      @NonNull TailoringReference<CatalogItem> source,
      @NonNull ReferenceAssembler referenceAssembler) {
    tailoringReferences.add(createTailoringReferenceDto(source, referenceAssembler));
  }

  @Override
  public Class<CatalogItem> getModelInterface() {
    return CatalogItem.class;
  }
}
