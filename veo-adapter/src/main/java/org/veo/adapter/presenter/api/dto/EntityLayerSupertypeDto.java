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
package org.veo.adapter.presenter.api.dto;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.veo.adapter.presenter.api.common.ModelObjectReference;
import org.veo.adapter.presenter.api.response.transformer.DtoToEntityContext;
import org.veo.core.entity.Domain;
import org.veo.core.entity.EntityLayerSupertype;
import org.veo.core.entity.Unit;

public interface EntityLayerSupertypeDto extends NameableDto, VersionedDto {

    Set<ModelObjectReference<Domain>> getDomains();

    ModelObjectReference<Unit> getOwner();

    void setDomains(Set<ModelObjectReference<Domain>> domains);

    void setOwner(ModelObjectReference<Unit> owner);

    Map<String, List<CustomLinkDto>> getLinks();

    void setLinks(Map<String, List<CustomLinkDto>> links);

    Map<String, CustomPropertiesDto> getCustomAspects();

    void setCustomAspects(Map<String, CustomPropertiesDto> customAspects);

    EntityLayerSupertype toEntity(DtoToEntityContext context);
}
