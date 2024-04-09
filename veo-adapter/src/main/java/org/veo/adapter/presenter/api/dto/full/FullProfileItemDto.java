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
package org.veo.adapter.presenter.api.dto.full;

import java.util.HashSet;
import java.util.Set;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import org.veo.adapter.presenter.api.Patterns;
import org.veo.adapter.presenter.api.common.ReferenceAssembler;
import org.veo.adapter.presenter.api.dto.AbstractProfileItemDto;
import org.veo.adapter.presenter.api.dto.CustomAspectMapDto;
import org.veo.adapter.presenter.api.dto.TailoringReferenceDto;
import org.veo.adapter.service.domaintemplate.dto.FullTemplateItemDto;
import org.veo.core.VeoConstants;
import org.veo.core.entity.Profile;
import org.veo.core.entity.ProfileItem;
import org.veo.core.entity.TailoringReference;
import org.veo.core.entity.TemplateItemAspects;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
public class FullProfileItemDto extends AbstractProfileItemDto
    implements FullTemplateItemDto<ProfileItem, Profile> {

  @Pattern(regexp = Patterns.UUID, message = VeoConstants.UUID_MESSAGE)
  @Schema(
      description = VeoConstants.UUID_MESSAGE,
      example = "adf037f1-0089-48ad-9177-92269918758b",
      format = "uuid")
  @ToString.Include
  private String id;

  @Valid @NotNull private TemplateItemAspects aspects = new TemplateItemAspects();

  @Valid
  @Schema(
      description = "Groups of customizable attributes - see '/schemas'",
      title = "CustomAspect")
  private CustomAspectMapDto customAspects = new CustomAspectMapDto();

  private Set<TailoringReferenceDto<ProfileItem, Profile>> tailoringReferences = new HashSet<>();

  public void add(
      @NonNull TailoringReference<ProfileItem, Profile> source,
      @NonNull ReferenceAssembler referenceAssembler) {
    tailoringReferences.add(createTailoringReferenceDto(source, referenceAssembler));
  }
}
