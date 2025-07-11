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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.veo.adapter.presenter.api.dto.QueryConditionDto;
import org.veo.adapter.presenter.api.dto.SingleValueQueryConditionDto;
import org.veo.core.entity.Client;
import org.veo.core.entity.ElementType;
import org.veo.core.repository.PagingConfiguration;
import org.veo.core.repository.QueryCondition;
import org.veo.core.repository.SingleValueQueryCondition;
import org.veo.core.usecase.base.GetElementsUseCase;
import org.veo.core.usecase.catalogitem.QueryCatalogItemsUseCase;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class QueryInputMapper {

  public static GetElementsUseCase.InputData map(
      Client client,
      UUID unitUuid,
      UUID domainId,
      String displayName,
      String subType,
      String status,
      List<UUID> childElementIds,
      Boolean hasChildElements,
      Boolean hasParentElements,
      UUID compositeId,
      UUID scopeId,
      String description,
      String designator,
      String name,
      String abbreviation,
      String updatedBy,
      PagingConfiguration<String> pagingConfiguration) {
    return GetElementsUseCase.InputData.builder()
        .authenticatedClient(client)
        .abbreviation(whereIn(abbreviation))
        .childElementIds(whereUuidIn(childElementIds))
        .compositeId(whereEquals(compositeId))
        .description(whereIn(description))
        .designator(whereIn(designator))
        .displayName(whereIn(displayName))
        .domainId(whereEquals(domainId))
        .hasChildElements(whereEquals(hasChildElements))
        .hasParentElements(whereEquals(hasParentElements))
        .name(whereIn(name))
        .pagingConfiguration(pagingConfiguration)
        .scopeId(whereEquals(scopeId))
        .status(whereEqualsOrNull(status))
        .subType(whereEqualsOrNull(subType))
        .unitUuid(whereUuidIn(unitUuid))
        .updatedBy(whereIn(updatedBy))
        .build();
  }

  public static GetElementsUseCase.InputData map(
      Client client,
      UUID domainId,
      UUID scopeId,
      Set<ElementType> elementTypes,
      PagingConfiguration<String> config) {
    return GetElementsUseCase.InputData.builder()
        .authenticatedClient(client)
        .elementTypes(whereIn(elementTypes))
        .domainId(whereEquals(domainId))
        .pagingConfiguration(config)
        .scopeId(whereEquals(scopeId))
        .build();
  }

  public static QueryCatalogItemsUseCase.InputData map(
      Client client,
      UUID domainId,
      ElementType elementType,
      String subType,
      String abbreviation,
      String name,
      String description,
      PagingConfiguration<String> config) {
    return new QueryCatalogItemsUseCase.InputData(
        client.getId(),
        domainId,
        config,
        whereEqualsOrNull(elementType),
        whereEqualsOrNull(subType),
        whereEqualsOrNull(abbreviation),
        whereEqualsOrNull(name),
        whereEqualsOrNull(description));
  }

  static <T> SingleValueQueryCondition<T> transformCondition(SingleValueQueryConditionDto<T> dto) {
    if (dto != null) {
      return new SingleValueQueryCondition<>(dto.getValue());
    }
    return null;
  }

  static QueryCondition<UUID> transformUuidCondition(QueryConditionDto<UUID> condition) {
    if (condition == null) {
      return null;
    }
    return new QueryCondition<>(condition.getValues());
  }

  static <T> QueryCondition<T> transformCondition(QueryConditionDto<T> filterDto) {
    if (filterDto != null) {
      return new QueryCondition<>(filterDto.getValues());
    }
    return null;
  }

  static <T> QueryCondition<T> whereIn(T value) {
    if (value != null) {
      return new QueryCondition<>(Set.of(value));
    }
    return null;
  }

  static <T> QueryCondition<T> whereIn(Collection<T> values) {
    if (values == null || values.isEmpty()) {
      return null;
    }
    return new QueryCondition<>(new HashSet<>(values));
  }

  static <T> SingleValueQueryCondition<T> whereEquals(T value) {
    if (value != null) {
      return new SingleValueQueryCondition<>(value);
    }
    return null;
  }

  static QueryCondition<UUID> whereUuidIn(List<UUID> ids) {
    if (ids != null) {
      return new QueryCondition<>(new HashSet<>(ids));
    }
    return null;
  }

  static QueryCondition<UUID> whereUuidIn(UUID id) {
    if (id != null) {
      return new QueryCondition<>(Set.of(id));
    }
    return null;
  }

  static QueryCondition<String> whereEqualsOrNull(String value) {
    if (value == null) {
      return null;
    }
    // Empty string -> match against null.
    if (value.isEmpty()) {
      return new QueryCondition<>(Collections.singleton(null));
    }
    return new QueryCondition<>(Set.of(value));
  }

  static QueryCondition<ElementType> whereEqualsOrNull(ElementType value) {
    if (value == null) {
      return null;
    }

    return new QueryCondition<>(Set.of(value));
  }
}
