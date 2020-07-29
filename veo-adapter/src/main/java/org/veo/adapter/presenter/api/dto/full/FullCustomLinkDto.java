/*******************************************************************************
 * Copyright (c) 2020 Jonas Jordan.
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
package org.veo.adapter.presenter.api.dto.full;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.veo.adapter.presenter.api.dto.AbstractCustomLinkDto;
import org.veo.adapter.presenter.api.response.IdentifiableDto;
import org.veo.adapter.presenter.api.response.transformer.DtoToEntityContext;
import org.veo.adapter.presenter.api.response.transformer.DtoToEntityTransformer;
import org.veo.adapter.presenter.api.response.transformer.EntityToDtoContext;
import org.veo.adapter.presenter.api.response.transformer.EntityToDtoTransformer;
import org.veo.core.entity.CustomLink;
import org.veo.core.entity.Key;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@Data
public class FullCustomLinkDto extends AbstractCustomLinkDto implements IdentifiableDto {

    @Pattern(regexp = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}",
             flags = Pattern.Flag.CASE_INSENSITIVE,
             message = "ID must be a valid UUID string following RFC 4122.")
    @Schema(description = "ID must be a valid UUID string following RFC 4122.",
            example = "adf037f1-0089-48ad-9177-92269918758b")
    @ToString.Include
    @NotNull
    private String id;

    @Schema(description = "A custom property which is determined by the requested entity schema - see '/schemas'",
            name = "customAspects",
            title = "CustomAspect")
    @Valid
    private Map<String, FullCustomPropertiesDto> customAspects = Collections.emptyMap();

    @Schema(name = "links",
            description = "Custom relations which do not affect the behavior.",
            title = "CustomLink")
    @Valid
    private Map<String, List<FullCustomLinkDto>> links = Collections.emptyMap();

    public static FullCustomLinkDto from(@Valid CustomLink customLink,
            EntityToDtoContext tcontext) {
        return EntityToDtoTransformer.transformCustomLink2Dto(tcontext, customLink);
    }

    public CustomLink toEntity(DtoToEntityContext tcontext) {
        return DtoToEntityTransformer.transformDto2CustomLink(tcontext, this, Key.uuidFrom(id));
    }
}
