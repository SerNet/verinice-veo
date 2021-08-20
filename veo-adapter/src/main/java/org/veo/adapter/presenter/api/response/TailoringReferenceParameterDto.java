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

import javax.validation.constraints.Size;

import org.veo.adapter.presenter.api.common.IdRef;
import org.veo.adapter.presenter.api.openapi.IdRefTailoringReferenceParameterReferencedCatalogable;
import org.veo.core.entity.Catalogable;
import org.veo.core.entity.CustomAspect;
import org.veo.core.entity.TailoringReferenceType;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Describes a Tailoringreference of this CatalogItem. It describes a link feature in the catalogItem which will be applied when the Catalogitem is incarnated. The referencedCatalogable need to be changed to link an acual element. ")
public class TailoringReferenceParameterDto {

    @Schema(required = true,
            implementation = IdRefTailoringReferenceParameterReferencedCatalogable.class)
    private IdRef<Catalogable> referencedCatalogable;

    @Schema(description = "The translatable key of the reference type. For customLinks this is the type of the link.",
            example = "proc_1")
    @Size(max = CustomAspect.TYPE_MAX_LENGTH)
    private String referenceKey;

    @Schema(description = "The type of the Tailoringreference.",
            example = "LINK or LINK_EXTERNAL",
            required = true)
    private TailoringReferenceType referenceType;
}
