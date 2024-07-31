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

import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import org.veo.core.entity.Client;
import org.veo.core.entity.Key;
import org.veo.core.entity.compliance.RequirementImplementation;
import org.veo.core.repository.PagedResult;
import org.veo.core.repository.PagingConfiguration;
import org.veo.core.repository.QueryCondition;
import org.veo.core.repository.RequirementImplementationQuery;
import org.veo.persistence.access.jpa.RequirementImplementationDataRepository;
import org.veo.persistence.entity.jpa.RequirementImplementationData;

/** Implements {@link RequirementImplementationQuery} using {@link Specification} API. */
public class RequirementImplementationQueryImpl implements RequirementImplementationQuery {

  private final RequirementImplementationDataRepository repo;
  protected Specification<RequirementImplementationData> spec;

  public RequirementImplementationQueryImpl(
      RequirementImplementationDataRepository repo, Client client) {
    this.repo = repo;
    spec = createSpecification(client);
  }

  @Override
  @Transactional(readOnly = true)
  public PagedResult<RequirementImplementation> execute(PagingConfiguration pagingConfiguration) {
    var items = repo.findAll(spec, toPageable(pagingConfiguration));
    return new PagedResult<>(
        pagingConfiguration,
        items.stream().map(RequirementImplementation.class::cast).toList(),
        items.getTotalElements(),
        items.getTotalPages());
  }

  @Override
  public void whereIdsIn(QueryCondition<Key<UUID>> ids) {
    spec = andIn(spec, "id", ids.map(Key::value));
  }

  private Specification<RequirementImplementationData> createSpecification(Client client) {
    return (root, query, criteriaBuilder) -> {
      query.distinct(true);
      root.fetch("control");
      return criteriaBuilder.equal(root.get("control").get("owner").get("client"), client);
    };
  }

  private static Pageable toPageable(PagingConfiguration pagingConfiguration) {
    return PageRequest.of(
        pagingConfiguration.getPageNumber(),
        pagingConfiguration.getPageSize(),
        pagingConfiguration.getSortOrder() == PagingConfiguration.SortOrder.ASCENDING
            ? Direction.ASC
            : Direction.DESC,
        pagingConfiguration.getSortColumn());
  }
}
