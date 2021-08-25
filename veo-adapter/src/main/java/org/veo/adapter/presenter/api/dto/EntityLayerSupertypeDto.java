/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Jonas Jordan.
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

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.veo.adapter.presenter.api.common.IdRef;
import org.veo.core.entity.Domain;
import org.veo.core.entity.ElementOwner;

public interface EntityLayerSupertypeDto extends NameableDto, VersionedDto, CatalogableDto {

    IdRef<ElementOwner> getOwner();

    void setDesignator(String designator);

    void setDomains(Set<IdRef<Domain>> domains);

    void setOwner(IdRef<ElementOwner> owner);

    Map<String, List<CustomLinkDto>> getLinks();

    void setLinks(Map<String, List<CustomLinkDto>> links);

    Map<String, CustomPropertiesDto> getCustomAspects();

    void setCustomAspects(Map<String, CustomPropertiesDto> customAspects);

    Map<String, String> getSubType();

    void setSubType(Map<String, String> subType);

    String getType();

    void setType(String type);
}
