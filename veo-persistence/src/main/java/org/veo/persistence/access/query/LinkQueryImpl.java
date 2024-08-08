/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2024  Jonas Jordan
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

import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import org.springframework.transaction.annotation.Transactional;

import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;
import org.veo.core.entity.InOrOutboundLink;
import org.veo.core.entity.LinkDirection;
import org.veo.core.repository.LinkQuery;
import org.veo.core.repository.PagedResult;
import org.veo.core.repository.PagingConfiguration;
import org.veo.persistence.access.jpa.ElementDataRepository;
import org.veo.persistence.entity.jpa.ElementData;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class LinkQueryImpl implements LinkQuery {
  public static final String WITH_LINK_SUB_QUERY =
      """
       with l as ((select 'INBOUND' as direction, type, customlink.domain_id, source_id as element_id
                   from customlink where target_id = :elementId and domain_id = :domainId)
                  union
                  (select 'OUTBOUND' as direction, type, customlink.domain_id, target_id as element_id
                   from customlink where source_id = :elementId and domain_id = :domainId))
""";
  private final EntityManager em;
  private final ElementDataRepository<ElementData> elementDataRepository;
  private final Element element;
  private final Domain domain;

  @Override
  @Transactional(readOnly = true)
  public PagedResult<InOrOutboundLink, SortCriterion> execute(
      PagingConfiguration<SortCriterion> pagingConfig) {
    var totalResultCount = (long) linkQuery("select count(*) from l;").getSingleResult();
    var totalPages = (int) Math.ceilDiv(totalResultCount, pagingConfig.getPageSize());
    List<Object[]> resultList =
        linkQuery(
                "select l.direction, l.type, l.element_id from l\n"
                    + join(pagingConfig.getSortColumn())
                    + order(pagingConfig.getSortColumn(), pagingConfig.getSortOrder())
                    + "limit :limit offset :offset;")
            .setParameter("limit", pagingConfig.getPageSize())
            .setParameter("offset", pagingConfig.getPageNumber() * pagingConfig.getPageSize())
            .getResultList();
    var linkedElementsById =
        elementDataRepository
            .findAllById(resultList.stream().map(row -> (UUID) row[2]).distinct().toList())
            .stream()
            .collect(Collectors.toMap(Element::getIdAsUUID, Function.identity()));
    var pageItems =
        resultList.stream()
            .map(
                row ->
                    new InOrOutboundLink(
                        LinkDirection.valueOf((String) row[0]),
                        (String) row[1],
                        linkedElementsById.get((UUID) row[2])))
            .toList();
    return new PagedResult<>(pagingConfig, pageItems, totalResultCount, totalPages);
  }

  private String order(SortCriterion sortColumn, PagingConfiguration.SortOrder sortOrder) {
    var col =
        switch (sortColumn) {
          case DIRECTION -> "l.direction";
          case LINKED_ELEMENT_ABBREVIATION -> "e.abbreviation";
          case LINKED_ELEMENT_NAME -> "e.name";
        };
    return "order by " + col + " " + sortOrder.getSqlKeyword() + "\n";
  }

  private String join(SortCriterion sortColumn) {
    if (sortColumn == SortCriterion.LINKED_ELEMENT_ABBREVIATION
        || sortColumn == SortCriterion.LINKED_ELEMENT_NAME) {
      return "inner join element as e on e.db_id = l.element_id\n";
    }
    return "";
  }

  private Query linkQuery(String query) {
    return em.createNativeQuery(WITH_LINK_SUB_QUERY + query)
        .setParameter("elementId", element.getIdAsUUID())
        .setParameter("domainId", domain.getIdAsUUID());
  }
}
