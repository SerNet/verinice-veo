/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2025  Aziz Khalledi
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
package org.veo.persistence.access.query;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.transaction.annotation.Transactional;

import org.veo.core.entity.CompositeElement;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;
import org.veo.core.repository.ElementQuery;
import org.veo.core.repository.PagedResult;
import org.veo.core.repository.PagingConfiguration;
import org.veo.core.repository.ParentElementQuery;
import org.veo.core.repository.QueryCondition;

import lombok.RequiredArgsConstructor;

/** Implements {@link ParentElementQuery} using {@link ElementQuery} with whereIdIn. */
@RequiredArgsConstructor
public class ParentElementQueryImpl implements ParentElementQuery {
  private final ElementQueryFactory elementQueryFactory;
  private final Element element;
  private final Domain domain;

  @Override
  @Transactional(readOnly = true)
  public PagedResult<Element, SortCriterion> execute(
      PagingConfiguration<SortCriterion> pagingConfig) {
    // Collect parent IDs (scopes and composites)
    Set<UUID> parentIds = new HashSet<>();

    parentIds.addAll(element.getScopes().stream().map(Element::getId).toList());

    if (element instanceof CompositeElement<?> compositeElement) {
      parentIds.addAll(compositeElement.getComposites().stream().map(Element::getId).toList());
    }

    if (parentIds.isEmpty()) {
      return new PagedResult<>(pagingConfig, List.of(), 0, 0);
    }

    // Use ElementQuery with whereIdIn for optimized retrieval
    ElementQuery<Element> query = elementQueryFactory.queryElements(domain.getOwner());
    query.whereIdIn(new QueryCondition<>(parentIds));
    query.whereDomainsContain(domain);
    PagingConfiguration<String> elementQueryConfig =
        new PagingConfiguration<>(
            pagingConfig.pageSize(),
            pagingConfig.pageNumber(),
            toColumnName(pagingConfig.sortColumn()),
            pagingConfig.sortOrder());

    var result = query.execute(elementQueryConfig);

    // Convert PagedResult<Element, String> to PagedResult<Element, SortCriterion>
    return new PagedResult<>(
        pagingConfig, result.resultPage(), result.totalResults(), result.totalPages());
  }

  private String toColumnName(SortCriterion sortCriterion) {
    if (sortCriterion == null) {
      return "name";
    }

    return switch (sortCriterion) {
      case TYPE -> "elementType";
      case ABBREVIATION -> "abbreviation";
      case NAME -> "name";
      case DESIGNATOR -> "designator";
    };
  }
}
