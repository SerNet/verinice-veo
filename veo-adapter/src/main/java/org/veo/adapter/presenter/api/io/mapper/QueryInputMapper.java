/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Jonas Jordan.
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

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.veo.adapter.presenter.api.dto.QueryConditionDto;
import org.veo.adapter.presenter.api.dto.SearchQueryDto;
import org.veo.adapter.presenter.api.dto.SingleValueQueryConditionDto;
import org.veo.adapter.presenter.api.dto.UuidQueryConditionDto;
import org.veo.core.entity.Client;
import org.veo.core.entity.Key;
import org.veo.core.repository.PagingConfiguration;
import org.veo.core.repository.QueryCondition;
import org.veo.core.repository.SingleValueQueryCondition;
import org.veo.core.usecase.base.GetElementsUseCase;
import org.veo.core.usecase.catalogitem.QueryCatalogItemsUseCase;

public class QueryInputMapper {

  public static GetElementsUseCase.InputData map(
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
      PagingConfiguration pagingConfiguration) {
    return new GetElementsUseCase.InputData(
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
        pagingConfiguration);
  }

  public static GetElementsUseCase.InputData map(
      Client client, SearchQueryDto searchQuery, PagingConfiguration pagingConfiguration) {
    return new GetElementsUseCase.InputData(
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
        pagingConfiguration);
  }

  public static QueryCatalogItemsUseCase.InputData map(
      Client client,
      String domainId,
      String elementType,
      String subType,
      PagingConfiguration config) {
    return new QueryCatalogItemsUseCase.InputData(
        client.getId(),
        Key.uuidFrom(domainId),
        config,
        createNonEmptyCondition(elementType),
        createNonEmptyCondition(subType));
  }

  static <T> SingleValueQueryCondition<T> transformCondition(SingleValueQueryConditionDto<T> dto) {
    if (dto != null) {
      return new SingleValueQueryCondition<>(dto.getValue());
    }
    return null;
  }

  static QueryCondition<Key<UUID>> transformCondition(UuidQueryConditionDto filterDto) {
    if (filterDto != null) {
      return new QueryCondition<>(
          filterDto.getValues().stream().map(Key::uuidFrom).collect(Collectors.toSet()));
    }
    return null;
  }

  static QueryCondition<Key<UUID>> transformUuidCondition(QueryConditionDto<String> condition) {
    if (condition == null) {
      return null;
    }
    return new QueryCondition<>(
        condition.getValues().stream().map(Key::uuidFrom).collect(Collectors.toSet()));
  }

  static <T> QueryCondition<T> transformCondition(QueryConditionDto<T> filterDto) {
    if (filterDto != null) {
      return new QueryCondition<>(filterDto.getValues());
    }
    return null;
  }

  static <T> SingleValueQueryCondition<T> createSingleValueCondition(T value) {
    if (value != null) {
      return new SingleValueQueryCondition<>(value);
    }
    return null;
  }

  static <T> QueryCondition<T> createNonEmptyCondition(T value) {
    if (value == null) {
      return null;
    }
    // Empty string -> match against null.
    if (value.equals("")) {
      return new QueryCondition<>(Collections.singleton(null));
    }
    return new QueryCondition<>(Set.of(value));
  }

  static QueryCondition<Key<UUID>> createUuidListCondition(List<String> ids) {
    if (ids != null) {
      return new QueryCondition<>(ids.stream().map(Key::uuidFrom).collect(Collectors.toSet()));
    }
    return null;
  }

  static QueryCondition<String> createStringFilter(String value) {
    if (value != null) {
      return new QueryCondition<>(Set.of(value));
    }
    return null;
  }

  static QueryCondition<Key<UUID>> createUuidCondition(String value) {
    if (value != null) {
      return new QueryCondition<>(Set.of(Key.uuidFrom(value)));
    }
    return null;
  }
}
