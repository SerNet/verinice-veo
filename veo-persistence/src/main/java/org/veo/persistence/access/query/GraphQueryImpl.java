/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2026  Alina Tsikunova
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
import java.util.Locale;
import java.util.UUID;

import jakarta.persistence.EntityManager;

import org.springframework.transaction.annotation.Transactional;

import org.veo.core.entity.ElementType;
import org.veo.core.repository.GraphQuery;
import org.veo.core.repository.RelationRow;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class GraphQueryImpl implements GraphQuery {

  private final EntityManager em;
  private final UUID elementId;
  private final UUID domainId;
  private final ElementType elementType;
  private final int limit;

  private static final String QUERY =
      """
        with all_relations as (

            -- customlink inbound
            select
                cl.source_id as neighbor_id,
                'CUSTOM_LINK' as relation_type,
                cl.source_id as source_id,
                cl.target_id as target_id,
                cl.type as link_type
            from customlink cl
            where cl.target_id = :elementId
            and cl.domain_id = :domainId

            union all

            -- customlink outbound
            select
                cl.target_id as neighbor_id,
                'CUSTOM_LINK' as relation_type,
                cl.source_id as source_id,
                cl.target_id as target_id,
                cl.type as link_type
            from customlink cl
            where cl.source_id = :elementId
            and cl.domain_id = :domainId

            union all

            -- member -> scope
            select
                sm.scope_id as neighbor_id,
                'PART_OR_MEMBER' as relation_type,
                sm.scope_id as source_id,
                sm.member_id as target_id,
                null as link_type
            from scope_members sm
            where sm.member_id = :elementId

            -- Placeholder for the dynamic _parts and scope block
            ${additionalRelations}
        ),

        filtered_relations as (
            select ar.*
            from all_relations ar
            join element_domain_association neighbor_eda
            on neighbor_eda.owner_db_id = ar.neighbor_id
            and neighbor_eda.domain_id = :domainId
        ),

        distinct_neighbors as (
            select distinct fr.neighbor_id
            from filtered_relations fr
        ),

        limited_neighbors as (
            select dn.neighbor_id
            from distinct_neighbors dn
            order by dn.neighbor_id
            limit :limit
        ),

        total_count_cte as (
            select count(*) as total_count
            from distinct_neighbors
        )

        select
            fr.neighbor_id,
            fr.relation_type,
            fr.source_id,
            fr.target_id,
            fr.link_type,
            tc.total_count
        from filtered_relations fr
        join limited_neighbors ln
        on ln.neighbor_id = fr.neighbor_id
        cross join total_count_cte tc
        order by fr.neighbor_id;
    """;

  private String additionalRelations(ElementType elementType) {
    if (elementType == ElementType.SCOPE) {
      return """
                union all

                -- scope -> member
                select
                    sm.member_id as neighbor_id,
                    'PART_OR_MEMBER' as relation_type,
                    sm.scope_id as source_id,
                    sm.member_id as target_id,
                    null as link_type
                from scope_members sm
                where sm.scope_id = :elementId
                """;
    }

    String table = elementType.name().toLowerCase(Locale.ROOT) + "_parts";

    return """
              union all

              -- composite -> part
              select
                  p.part_id as neighbor_id,
                  'PART_OR_MEMBER' as relation_type,
                  p.composite_id as source_id,
                  p.part_id as target_id,
                  null as link_type
              from ${table} p
              where p.composite_id = :elementId

              union all

              -- part -> composite
              select
                  p.composite_id as neighbor_id,
                  'PART_OR_MEMBER' as relation_type,
                  p.composite_id as source_id,
                  p.part_id as target_id,
                  null as link_type
              from ${table} p
              where p.part_id = :elementId
              """
        .replace("${table}", table);
  }

  private String buildQuery(ElementType elementType) {
    return QUERY.replace("${additionalRelations}", additionalRelations(elementType));
  }

  @Override
  @SuppressWarnings("unchecked")
  @Transactional()
  public List<RelationRow> execute() {
    List<Object[]> rows =
        em.createNativeQuery(buildQuery(elementType))
            .setParameter("elementId", elementId)
            .setParameter("domainId", domainId)
            .setParameter("limit", limit)
            .getResultList();
    return rows.stream().map(this::toRelationRow).toList();
  }

  private RelationRow toRelationRow(Object[] row) {
    return new RelationRow(
        (UUID) row[0],
        (String) row[1],
        (UUID) row[2],
        (UUID) row[3],
        (String) row[4],
        ((Number) row[5]).longValue());
  }
}
