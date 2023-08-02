/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Alexander Koderman
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
package org.veo.adapter.presenter.api.io.mapper;

import static org.veo.adapter.presenter.api.io.mapper.QueryInputMapper.createNonEmptyCondition;
import static org.veo.adapter.presenter.api.io.mapper.QueryInputMapper.createSingleValueCondition;
import static org.veo.adapter.presenter.api.io.mapper.QueryInputMapper.createStringFilter;
import static org.veo.adapter.presenter.api.io.mapper.QueryInputMapper.createUuidCondition;
import static org.veo.adapter.presenter.api.io.mapper.QueryInputMapper.createUuidListCondition;
import static org.veo.adapter.presenter.api.io.mapper.QueryInputMapper.transformCondition;
import static org.veo.adapter.presenter.api.io.mapper.QueryInputMapper.transformUuidCondition;

import java.util.List;

import org.veo.adapter.presenter.api.dto.SearchQueryDto;
import org.veo.core.entity.Client;
import org.veo.core.entity.Key;
import org.veo.core.repository.PagingConfiguration;
import org.veo.core.usecase.base.GetElementsUseCase;

public class GetRiskAffectedInputMapper {
  public static GetElementsUseCase.RiskAffectedInputData map(
      Client client,
      String unitUuid,
      String domainId,
      String displayName,
      String subType,
      String status,
      List<String> childElementIds,
      Boolean hasChildElements,
      Boolean hasParentElements,
      String compositeId,
      String scopeId,
      String description,
      String designator,
      String name,
      String updatedBy,
      PagingConfiguration pagingConfiguration,
      boolean embedRisks) {
    return new GetElementsUseCase.RiskAffectedInputData(
        client,
        createUuidCondition(unitUuid),
        createSingleValueCondition(Key.uuidFrom(domainId)),
        createStringFilter(displayName),
        createNonEmptyCondition(subType),
        createNonEmptyCondition(status),
        createUuidListCondition(childElementIds),
        createSingleValueCondition(hasChildElements),
        createSingleValueCondition(hasParentElements),
        createSingleValueCondition(Key.uuidFrom(compositeId)),
        createSingleValueCondition(Key.uuidFrom(scopeId)),
        createStringFilter(description),
        createStringFilter(designator),
        createStringFilter(name),
        createStringFilter(updatedBy),
        pagingConfiguration,
        embedRisks);
  }

  public static GetElementsUseCase.RiskAffectedInputData map(
      Client client,
      SearchQueryDto searchQuery,
      PagingConfiguration pagingConfiguration,
      boolean embedRisks) {
    return new GetElementsUseCase.RiskAffectedInputData(
        client,
        transformCondition(searchQuery.getUnitId()),
        null,
        transformCondition(searchQuery.getDisplayName()),
        transformCondition(searchQuery.getSubType()),
        transformCondition(searchQuery.getStatus()),
        transformUuidCondition(searchQuery.getChildElementIds()),
        transformCondition(searchQuery.getHasChildElements()),
        transformCondition(searchQuery.getHasParentElements()),
        null,
        null,
        transformCondition(searchQuery.getDescription()),
        transformCondition(searchQuery.getDesignator()),
        transformCondition(searchQuery.getName()),
        transformCondition(searchQuery.getUpdatedBy()),
        pagingConfiguration,
        embedRisks);
  }
}
