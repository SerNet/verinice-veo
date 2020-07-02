/*******************************************************************************
 * Copyright (c) 2019 Urs Zeidler.
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.veo.adapter.presenter.api.response;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import io.swagger.v3.oas.annotations.media.Schema;

import org.veo.adapter.presenter.api.common.ModelObjectReference;
import org.veo.adapter.presenter.api.response.transformer.DtoEntityToTargetContext;
import org.veo.adapter.presenter.api.response.transformer.DtoEntityToTargetTransformer;
import org.veo.adapter.presenter.api.response.transformer.DtoTargetToEntityContext;
import org.veo.adapter.presenter.api.response.transformer.DtoTargetToEntityTransformer;
import org.veo.core.entity.Domain;
import org.veo.core.entity.ModelObject;

/**
 * Transfer object for complete Domains.
 *
 * Contains all information of the Domain.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
public class DomainDto extends BaseModelObjectDto implements NameAbleDto {

    @NotNull(message = "A name must be present.")
    @Schema(description = "The name for the Domain.", example = "Data protection", required = true)
    @ToString.Include
    private String name;

    @Schema(description = "The abbreviation for the Domain.",
            example = "Data prot.",
            required = false)
    private String abbreviation;

    @Schema(description = "The description for the Domain.",
            example = "Everything around data protection.",
            required = false)
    private String description;

    @Schema(description = "The active for the Domain.")
    private Boolean active;

    public Collection<ModelObjectReference<? extends ModelObject>> getReferences() {
        List<ModelObjectReference<? extends ModelObject>> list = new ArrayList<>();
        return list;
    }

    public Boolean isActive() {
        return active;
    }

    public static DomainDto from(@Valid Domain domain, DtoEntityToTargetContext tcontext) {
        return DtoEntityToTargetTransformer.transformDomain2Dto(tcontext, domain);
    }

    public Domain toDomain(DtoTargetToEntityContext tcontext) {
        return DtoTargetToEntityTransformer.transformDto2Domain(tcontext, this);
    }
}
