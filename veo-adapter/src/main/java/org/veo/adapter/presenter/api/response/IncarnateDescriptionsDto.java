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
import org.veo.adapter.presenter.api.common.ReferenceAssembler;
import org.veo.core.usecase.parameter.IncarnateCatalogItemDescription;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IncarnateDescriptionsDto {

    @Schema(description = "A list of parameters to apply to an item", required = true)
    private List<IncarnateCatalogItemDescriptionDto> parameters;

    public IncarnateDescriptionsDto(List<IncarnateCatalogItemDescription> references,
            ReferenceAssembler urlAssembler) {
        this.parameters = references.stream()
                                    .map(p -> new IncarnateCatalogItemDescriptionDto(p,
                                            urlAssembler))
                                    .collect(Collectors.toList());
    }

    public List<IncarnateCatalogItemDescription> dto2Model(IdRefResolver idRefResolver) {
        return getParameters().stream()
                              .map(a -> a.dto2Model(idRefResolver))
                              .collect(Collectors.toList());
    }
}
