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
package org.veo.adapter.presenter.api.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.veo.adapter.presenter.api.common.IdRef;
import org.veo.adapter.presenter.api.common.ReferenceAssembler;
import org.veo.adapter.presenter.api.openapi.IdRefTemplateItem;
import org.veo.core.entity.Identifiable;
import org.veo.core.entity.TemplateItem;
import org.veo.core.entity.ref.ITypedId;
import org.veo.core.entity.state.TailoringReferenceParameterState;
import org.veo.core.entity.state.TemplateItemIncarnationDescriptionState;
import org.veo.core.usecase.parameter.TemplateItemIncarnationDescription;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Describes the incarnation parameters of one template item.")
public class IncarnateTemplateItemDescriptionDto<
        T extends TemplateItem<T, TNamespace>, TNamespace extends Identifiable>
    implements TemplateItemIncarnationDescriptionState<T, TNamespace> {

  @Schema(
      title = "Reference the template item to be incarnated.",
      requiredMode = REQUIRED,
      implementation = IdRefTemplateItem.class)
  private IdRef<TemplateItem<?, ?>> item;

  @ArraySchema(
      schema =
          @Schema(
              title = "A list of references this element needs to set.",
              implementation = TailoringReferenceParameterDto.class))
  private List<TailoringReferenceParameterDto> references;

  public IncarnateTemplateItemDescriptionDto(
      TemplateItemIncarnationDescription<T, TNamespace> p, ReferenceAssembler urlAssembler) {
    item = IdRef.from(p.getItem(), urlAssembler);
    references =
        p.getReferences().stream()
            .map(
                r ->
                    new TailoringReferenceParameterDto(
                        IdRef.from(r.getReferencedElement(), urlAssembler),
                        r.getReferenceKey(),
                        r.getReferenceType(),
                        r.getId()))
            .toList();
  }

  @JsonIgnore
  @Override
  public ITypedId<TemplateItem<?, ?>> getItemRef() {
    return item;
  }

  @JsonIgnore
  @Override
  public List<TailoringReferenceParameterState> getParameterStates() {
    return references.stream().map(TailoringReferenceParameterState.class::cast).toList();
  }
}
