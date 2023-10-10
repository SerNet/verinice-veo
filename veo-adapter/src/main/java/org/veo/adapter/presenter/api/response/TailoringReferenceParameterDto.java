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

import java.util.Optional;

import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.veo.adapter.presenter.api.common.IdRef;
import org.veo.adapter.presenter.api.openapi.IdRefTailoringReferenceParameterReferencedElement;
import org.veo.core.entity.CustomAspect;
import org.veo.core.entity.Element;
import org.veo.core.entity.TailoringReferenceType;
import org.veo.core.entity.ref.ITypedId;
import org.veo.core.entity.state.TailoringReferenceParameterState;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.AccessMode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(
    description =
        "Describes a reference of this element. "
            + "It describes a link feature in the catalogItem which will be applied "
            + "when the Catalogitem is incarnated. The referencedCatalogable needs "
            + "to be changed to link an actual element. ")
public class TailoringReferenceParameterDto implements TailoringReferenceParameterState {

  @Schema(
      description =
          "The actual reference to an existing element "
              + "in the unit(it may be set or left like it is), or NULL "
              + "when this reference should be resolved internally.",
      accessMode = AccessMode.READ_WRITE,
      implementation = IdRefTailoringReferenceParameterReferencedElement.class)
  private IdRef<Element> referencedElement;

  @Schema(
      description =
          "The translatable key of the reference type. For customLinks "
              + "this is the type of the link.",
      example = "proc_1",
      accessMode = AccessMode.READ_ONLY)
  @Size(max = CustomAspect.TYPE_MAX_LENGTH)
  private String referenceKey;

  @Schema(
      description = "The type of the Tailoringreference.",
      example = "LINK or LINK_EXTERNAL",
      accessMode = AccessMode.READ_ONLY,
      requiredMode = REQUIRED)
  private TailoringReferenceType referenceType;

  @Schema(
      description = "The maping key of the Tailoringreference.",
      example = "an uuid",
      accessMode = AccessMode.READ_ONLY,
      requiredMode = REQUIRED)
  private String id;

  @JsonIgnore
  @Override
  public Optional<ITypedId<Element>> getReferencedElementRef() {
    return Optional.ofNullable(referencedElement);
  }
}
