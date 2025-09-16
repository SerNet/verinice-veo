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

import java.util.List;
import java.util.UUID;

import org.apache.commons.collections4.ListUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import org.veo.core.VeoConstants;
import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
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
  public PagedResult<RequirementImplementation, String> execute(
      PagingConfiguration<String> pagingConfiguration) {
    var items = repo.findAll(spec, toPageable(pagingConfiguration));
    fullyLoadItems(items.stream().map(RequirementImplementationData::getDbId).toList());
    return new PagedResult<>(
        pagingConfiguration,
        items.stream().map(RequirementImplementation.class::cast).toList(),
        items.getTotalElements(),
        items.getTotalPages());
  }

  private void fullyLoadItems(List<Long> dbIds) {
    ListUtils.partition(dbIds, VeoConstants.DB_QUERY_CHUNK_SIZE)
        .forEach(repo::findAllByDbIdsWithControls);
  }

  @Override
  public void whereIdsIn(QueryCondition<UUID> ids) {
    spec = andIn(spec, "id", ids);
  }

  @Override
  public void whereControlInDomain(Domain domain) {
    spec =
        spec.and(
            (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(
                    root.get("control").get("domainAssociations").get("domain"), domain));
  }

  private Specification<RequirementImplementationData> createSpecification(Client client) {
    return (root, query, criteriaBuilder) -> {
      root.join("control");
      return criteriaBuilder.and(
          criteriaBuilder.isEmpty(root.get("control").get("parts")),
          criteriaBuilder.equal(root.get("control").get("owner").get("client"), client));
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
