/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2024  Aziz Khalledi
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

import java.util.UUID;

import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.core.entity.compliance.ControlImplementation;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.repository.ControlImplementationQuery;
import org.veo.core.repository.PagedResult;
import org.veo.core.repository.PagingConfiguration;
import org.veo.persistence.access.jpa.ControlImplementationDataRepository;
import org.veo.persistence.entity.jpa.ControlImplementationData;

/** Implements {@link ControlImplementationQuery} using {@link Specification} API. */
public class ControlImplementationQueryImpl implements ControlImplementationQuery {

  private final ControlImplementationDataRepository repo;
  protected Specification<ControlImplementationData> spec;

  public ControlImplementationQueryImpl(
      ControlImplementationDataRepository repo, Client client, UUID domainId) {
    this.repo = repo;
    spec = createSpecification(client, domainId);
  }

  @Override
  @Transactional(readOnly = true)
  public PagedResult<ControlImplementation, String> execute(
      PagingConfiguration<String> pagingConfiguration) {
    var items = repo.findAll(spec, toPageable(pagingConfiguration));
    return new PagedResult<>(
        pagingConfiguration,
        items.stream().map(ControlImplementation.class::cast).toList(),
        items.getTotalElements(),
        items.getTotalPages());
  }

  @Override
  public void whereControlIdIn(UUID controlId) {
    spec =
        spec.and(
            (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("control").get("id"), controlId));
  }

  @Override
  public void whereControlhasSubType(String subtype, UUID domainId) {
    spec =
        spec.and(
            (root, query, criteriaBuilder) -> {
              var join =
                  root.join("control", JoinType.INNER).join("domainAssociations", JoinType.LEFT);
              return criteriaBuilder.and(
                  criteriaBuilder.equal(join.get("domain").get("id"), domainId),
                  criteriaBuilder.in(join.get("subType")).value(subtype));
            });
  }

  @Override
  public void whereRiskAffectedIs(UUID riskAffectedId) {
    spec =
        spec.and(
            (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("owner").get("id"), riskAffectedId));
  }

  private Specification<ControlImplementationData> createSpecification(
      Client client, UUID domainId) {
    return (root, query, criteriaBuilder) -> {
      if (!currentQueryIsCountRecords(query)) {
        root.fetch("control");
        root.fetch("owner");
        root.fetch("responsible", JoinType.LEFT);
      }

      boolean domainExists = client.getDomains().stream().anyMatch(d -> d.getId().equals(domainId));

      if (!domainExists) {
        throw new NotFoundException(domainId, Domain.class);
      }

      Predicate riskAffectedPredicate =
          criteriaBuilder.equal(
              root.get("owner").get("domainAssociations").get("domain").get("id"), domainId);
      Predicate domainPredicate =
          criteriaBuilder.equal(
              root.get("control").get("domainAssociations").get("domain").get("id"), domainId);
      Predicate clientPredicate =
          criteriaBuilder.equal(root.get("control").get("owner").get("client"), client);

      return criteriaBuilder.and(clientPredicate, domainPredicate, riskAffectedPredicate);
    };
  }

  /**
   * Determines if the query is for counting records (returns `Long`). This is important because
   * using Paging with JPA executes a count query. If JOIN FETCH is used, it can cause an
   * `org.hibernate.QueryException` when the current page returns 0 elements, due to an empty left
   * side of the JOIN FETCH. For more info, refer to this <a
   * href="https://medium.com/@aleksanderkolata/usecase-01-spring-data-jpa-jpa-specification-how-pagination-can-destroy-your-weekend-9572397bbd74">
   * article</a>.
   *
   * @param criteriaQuery the JPA criteria query
   * @return true if the query is a count query, false otherwise
   */
  private boolean currentQueryIsCountRecords(CriteriaQuery<?> criteriaQuery) {
    return criteriaQuery.getResultType() == Long.class
        || criteriaQuery.getResultType() == long.class;
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
