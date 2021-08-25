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

import java.util.List;
import java.util.stream.Collectors;

import org.veo.adapter.IdRefResolver;
import org.veo.adapter.presenter.api.common.IdRef;
import org.veo.adapter.presenter.api.common.ReferenceAssembler;
import org.veo.adapter.presenter.api.openapi.IdRefCatalogItemDescriptionItem;
import org.veo.core.entity.CatalogItem;
import org.veo.core.usecase.parameter.IncarnateCatalogItemDescription;
import org.veo.core.usecase.parameter.TailoringReferenceParameter;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Describes the incarnation parameters of the item.")
public class IncarnateCatalogItemDescriptionDto {

    @Schema(required = true, implementation = IdRefCatalogItemDescriptionItem.class)
    private IdRef<CatalogItem> item;
    @ArraySchema(schema = @Schema(implementation = TailoringReferenceParameterDto.class))
    private List<TailoringReferenceParameterDto> references;

    public IncarnateCatalogItemDescriptionDto(IncarnateCatalogItemDescription p,
            ReferenceAssembler urlAssembler) {
        item = IdRef.from(p.getItem(), urlAssembler);
        references = p.getReferences()
                      .stream()
                      .map(r -> new TailoringReferenceParameterDto(
                              IdRef.from(r.getReferencedCatalogable(), urlAssembler),
                              r.getReferenceKey(), r.getReferenceType()))
                      .collect(Collectors.toList());

    }

    public IncarnateCatalogItemDescription dto2Model(IdRefResolver idRefResolver) {
        List<TailoringReferenceParameter> list = getReferences().stream()
                                                                .map(t -> new TailoringReferenceParameter(
                                                                        t.getReferencedCatalogable() == null
                                                                                ? null
                                                                                : idRefResolver.resolve(t.getReferencedCatalogable()),
                                                                        t.getReferenceType(),
                                                                        t.getReferenceKey()))
                                                                .collect(Collectors.toList());
        return new IncarnateCatalogItemDescription(idRefResolver.resolve(item), list);
    }

}
