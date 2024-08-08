/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Jonas Jordan.
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

import static org.veo.persistence.access.query.QueryFunctions.andIn;
import static org.veo.persistence.access.query.QueryFunctions.andInIgnoringCase;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import org.veo.core.entity.CatalogItem;
import org.veo.core.entity.Domain;
import org.veo.core.repository.CatalogItemQuery;
import org.veo.core.repository.PagedResult;
import org.veo.core.repository.PagingConfiguration;
import org.veo.core.repository.QueryCondition;
import org.veo.persistence.access.jpa.CatalogItemDataRepository;
import org.veo.persistence.entity.jpa.CatalogItemData;

/** Implements {@link CatalogItemQuery} using {@link Specification} API. */
public class CatalogItemQueryImpl implements CatalogItemQuery {

  private final CatalogItemDataRepository repo;
  protected Specification<CatalogItemData> spec;

  public CatalogItemQueryImpl(CatalogItemDataRepository repo, Domain domain) {
    this.repo = repo;
    spec = createSpecification(domain);
  }

  @Override
  @Transactional(readOnly = true)
  public PagedResult<CatalogItem, String> execute(PagingConfiguration<String> pagingConfiguration) {
    var items = repo.findAll(spec, toPageable(pagingConfiguration));
    return new PagedResult<>(
        pagingConfiguration,
        items.stream().map(CatalogItem.class::cast).toList(),
        items.getTotalElements(),
        items.getTotalPages());
  }

  @Override
  public void whereElementTypeMatches(QueryCondition<String> elementTypes) {
    spec = andIn(spec, "elementType", elementTypes);
  }

  @Override
  public void whereSubTypeMatches(QueryCondition<String> subTypes) {
    spec = andIn(spec, "subType", subTypes);
  }

  @Override
  public void whereAbbreviationMatchesIgnoreCase(QueryCondition<String> condition) {
    spec = andInIgnoringCase("abbreviation", condition, spec);
  }

  @Override
  public void whereNameMatchesIgnoreCase(QueryCondition<String> condition) {
    spec = andInIgnoringCase("name", condition, spec);
  }

  @Override
  public void whereDescriptionMatchesIgnoreCase(QueryCondition<String> condition) {
    spec = andInIgnoringCase("description", condition, spec);
  }

  private Specification<CatalogItemData> createSpecification(Domain domain) {
    return (root, query, criteriaBuilder) -> {
      query.distinct(true);
      return criteriaBuilder.equal(root.join("domain"), domain);
    };
  }

  private static Pageable toPageable(PagingConfiguration<String> pagingConfiguration) {
    return PageRequest.of(
        pagingConfiguration.getPageNumber(),
        pagingConfiguration.getPageSize(),
        pagingConfiguration.getSortOrder() == PagingConfiguration.SortOrder.ASCENDING
            ? Direction.ASC
            : Direction.DESC,
        pagingConfiguration.getSortColumn());
  }
}
