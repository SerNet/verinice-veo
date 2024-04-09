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
package org.veo.adapter.presenter.api.dto;

import java.util.Optional;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.veo.adapter.presenter.api.common.IdRef;
import org.veo.adapter.presenter.api.common.ReferenceAssembler;
import org.veo.adapter.presenter.api.dto.full.LinkTailoringReferenceDto;
import org.veo.adapter.service.domaintemplate.dto.ControlImplementationTailoringReferenceDto;
import org.veo.adapter.service.domaintemplate.dto.RiskTailoringReferenceDto;
import org.veo.core.entity.ControlImplementationTailoringReference;
import org.veo.core.entity.Identifiable;
import org.veo.core.entity.LinkTailoringReference;
import org.veo.core.entity.RiskTailoringReference;
import org.veo.core.entity.TailoringReference;
import org.veo.core.entity.TemplateItem;
import org.veo.core.entity.aspects.SubTypeAspect;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public abstract class AbstractTemplateItemDto<
        T extends TemplateItem<T, TNamespace>, TNamespace extends Identifiable>
    extends AbstractVersionedSelfReferencingDto implements NameableDto {
  @NotNull private String name;

  private String abbreviation;

  private String description;

  @NotNull
  @Schema(description = "Entity type identifier", example = "person")
  @Size(min = 1, max = 32)
  private String elementType;

  @NotNull
  @Schema(description = "The subtype for the Element.", example = "PER")
  @Size(min = 1, max = SubTypeAspect.SUB_TYPE_MAX_LENGTH)
  private String subType;

  protected TailoringReferenceDto<T, TNamespace> createTailoringReferenceDto(
      TailoringReference<T, TNamespace> source, ReferenceAssembler referenceAssembler) {
    var target = supplyDto(source, referenceAssembler);
    target.setId(source.getIdAsString());
    target.setReferenceType(source.getReferenceType());
    target.setTarget(IdRef.from(source.getTarget(), referenceAssembler));
    return target;
  }

  private TailoringReferenceDto<T, TNamespace> supplyDto(
      TailoringReference<T, TNamespace> source, ReferenceAssembler uriAssembler) {
    if (source instanceof LinkTailoringReference<T, TNamespace> linkSource) {
      var linkRefDto = new LinkTailoringReferenceDto<T, TNamespace>();
      linkRefDto.setLinkType(linkSource.getLinkType());
      linkRefDto.setAttributes(linkSource.getAttributes());
      return linkRefDto;
    } else if (source instanceof RiskTailoringReference<T, TNamespace> riskSource) {
      var riskRefDto = new RiskTailoringReferenceDto<T, TNamespace>();
      Optional.ofNullable(riskSource.getMitigation())
          .map(m -> IdRef.from(m, uriAssembler))
          .ifPresent(riskRefDto::setMitigation);
      Optional.ofNullable(riskSource.getRiskOwner())
          .map(p -> IdRef.from(p, uriAssembler))
          .ifPresent(riskRefDto::setRiskOwner);
      riskRefDto.setRiskDefinitions(riskSource.getRiskDefinitions());
      return riskRefDto;
    } else if (source instanceof ControlImplementationTailoringReference<T, TNamespace> ciSource) {
      var ciRefDto = new ControlImplementationTailoringReferenceDto<T, TNamespace>();
      Optional.ofNullable(ciSource.getResponsible())
          .map(r -> IdRef.from(r, uriAssembler))
          .ifPresent(ciRefDto::setResponsible);
      ciRefDto.setDescription(ciSource.getDescription());
      return ciRefDto;
    }
    return new TailoringReferenceDto<>();
  }
}
