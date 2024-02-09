/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2024 Alexander Koderman
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
package org.veo.persistence.access;

import static org.springframework.transaction.annotation.Isolation.SERIALIZABLE;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import org.veo.persistence.entity.FlyweightLink;

import lombok.AllArgsConstructor;

@Repository
@AllArgsConstructor
public class FlyweightLinkDataRepositoryImpl {

  @PersistenceContext private final EntityManager em;

  private Set<FlyweightLink> findAllLinks(
      Set<String> types, String domainId, String clientId, String unitId) {

    String sql =
        "SELECT l.source_id AS sourceId, e.dtype AS sourceType, l.target_id AS targetId, l.type "
            + "FROM customlink l, element e, unit u "
            + "WHERE l.type IN :types AND l.domain_id = :domainId "
            + "AND l.source_id = e.db_id "
            + "AND e.owner_id = u.db_id "
            + "AND u.client_id = :clientId "
            + "AND u.db_id = :unitId";

    Query query = em.createNativeQuery(sql);
    query.setParameter("types", types);
    query.setParameter("domainId", domainId);
    query.setParameter("clientId", clientId);
    query.setParameter("unitId", unitId);

    return ((List<Object[]>) query.getResultList())
        .stream()
            .map(
                result ->
                    new FlyweightLink(
                        (String) result[0],
                        (String) result[1],
                        (String) result[2],
                        (String) result[3]))
            .collect(Collectors.toSet());
  }

  @Transactional(readOnly = true, isolation = SERIALIZABLE)
  public Set<FlyweightElement> findAllLinksGroupedByElement(
      Set<String> types, String domainId, String clientId, String unitId) {
    return findAllLinks(types, domainId, clientId, unitId).stream()
        .collect(Collectors.groupingBy(FlyweightLink::sourceId))
        .entrySet()
        .stream()
        .map(
            entry ->
                new FlyweightElement(
                    entry.getKey(),
                    entry.getValue().getFirst().sourceType(),
                    new HashSet<>(entry.getValue())))
        .collect(Collectors.toSet());
  }
}
