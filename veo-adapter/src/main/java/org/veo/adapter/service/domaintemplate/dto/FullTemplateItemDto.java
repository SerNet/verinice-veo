/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Jonas Jordan
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

import java.util.Set;
import java.util.UUID;

import jakarta.validation.Valid;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import org.veo.adapter.presenter.api.common.ReferenceAssembler;
import org.veo.adapter.presenter.api.dto.CustomAspectMapDto;
import org.veo.adapter.presenter.api.dto.NameableDto;
import org.veo.adapter.presenter.api.dto.TailoringReferenceDto;
import org.veo.adapter.presenter.api.dto.full.LinkTailoringReferenceDto;
import org.veo.core.entity.ElementType;
import org.veo.core.entity.Identifiable;
import org.veo.core.entity.TailoringReference;
import org.veo.core.entity.TemplateItem;
import org.veo.core.entity.TemplateItemAspects;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.NonNull;

public interface FullTemplateItemDto<
        T extends TemplateItem<T, TNamespace>, TNamespace extends Identifiable>
    extends NameableDto {
  String getStatus();

  CustomAspectMapDto getCustomAspects();

  void setSymbolicId(UUID id);

  void setStatus(String status);

  void setCustomAspects(CustomAspectMapDto customAspects);

  ElementType getElementType();

  void setElementType(ElementType elementType);

  String getSubType();

  void setSubType(String subType);

  TemplateItemAspects getAspects();

  void setAspects(TemplateItemAspects aspects);

  @JsonTypeInfo(
      use = JsonTypeInfo.Id.NAME,
      visible = true,
      defaultImpl = TailoringReferenceDto.class,
      include = JsonTypeInfo.As.EXISTING_PROPERTY,
      property = "referenceType")
  @JsonSubTypes({
    @JsonSubTypes.Type(value = LinkTailoringReferenceDto.class, name = "LINK_EXTERNAL"),
    @JsonSubTypes.Type(value = LinkTailoringReferenceDto.class, name = "LINK"),
    @JsonSubTypes.Type(value = RiskTailoringReferenceDto.class, name = "RISK"),
    @JsonSubTypes.Type(
        value = ControlImplementationTailoringReferenceDto.class,
        name = "CONTROL_IMPLEMENTATION"),
    @JsonSubTypes.Type(
        value = RequirementImplementationTailoringReferenceDto.class,
        name = "REQUIREMENT_IMPLEMENTATION"),
  })
  @Schema(description = "References to other catalog items in the same domain")
  Set<TailoringReferenceDto<T, TNamespace>> getTailoringReferences();

  void setTailoringReferences(Set<TailoringReferenceDto<T, TNamespace>> tailoringReferences);

  void add(
      @Valid @NonNull TailoringReference<T, TNamespace> source,
      @NonNull ReferenceAssembler uriAssembler);
}
