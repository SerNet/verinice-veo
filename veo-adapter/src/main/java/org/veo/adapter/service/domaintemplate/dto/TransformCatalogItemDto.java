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

import java.util.Set;

import javax.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;

import org.veo.adapter.presenter.api.Patterns;
import org.veo.adapter.presenter.api.dto.AbstractTailoringReferenceDto;
import org.veo.adapter.presenter.api.dto.composite.CompositeCatalogItemDto;
import org.veo.adapter.presenter.api.dto.create.CreateTailoringReferenceDto;
import org.veo.adapter.presenter.api.response.IdentifiableDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * This DTO represent a complete catalogItem, i.e. it contains a FullCXXXDto for
 * the element of this item, it is primarily used in the construction and
 * serialization of a domain template.
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class TransformCatalogItemDto extends CompositeCatalogItemDto implements IdentifiableDto {

    @Pattern(regexp = Patterns.UUID, message = "ID must be a valid UUID string following RFC 4122.")
    @Schema(description = "ID must be a valid UUID string following RFC 4122.",
            example = "adf037f1-0089-48ad-9177-92269918758b")
    @ToString.Include
    private String id;

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
                  visible = true,
                  defaultImpl = CreateTailoringReferenceDto.class,
                  include = As.EXISTING_PROPERTY,
                  property = "referenceType")
    @JsonSubTypes({ @JsonSubTypes.Type(value = TransformExternalTailoringReference.class,
                                       name = "LINK_EXTERNAL") })
    @Override
    public void setTailoringReferences(Set<AbstractTailoringReferenceDto> tailoringReferences) {
        super.setTailoringReferences(tailoringReferences);
    }
}
