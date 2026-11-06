/*
 * verinice.veo
 * Copyright (C) 2026  Jonas Jordan
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
 */
package org.veo.persistence.access.query;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import org.jspecify.annotations.NonNull;
import org.springframework.transaction.annotation.Transactional;

import org.veo.core.UserAccessRights;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;
import org.veo.core.entity.Person;
import org.veo.core.entity.Task;
import org.veo.core.entity.TaskType;
import org.veo.core.entity.TranslatedText;
import org.veo.core.entity.Unit;
import org.veo.core.entity.compliance.RequirementImplementation;
import org.veo.core.entity.inspection.Severity;
import org.veo.core.entity.ref.TypedId;
import org.veo.core.repository.PagedResult;
import org.veo.core.repository.PagingConfiguration;
import org.veo.core.repository.QueryCondition;
import org.veo.core.repository.TaskQuery;
import org.veo.persistence.access.jpa.ElementDataRepository;
import org.veo.persistence.access.jpa.PersonDataRepository;
import org.veo.persistence.access.jpa.RequirementImplementationDataRepository;
import org.veo.persistence.entity.jpa.ElementData;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.json.JsonMapper;

@RequiredArgsConstructor
public class TaskQueryImpl implements TaskQuery {
  public static final int COL_TASK_TYPE = 0;
  public static final int COL_ASSIGNEE_ID = 1;
  public static final int COL_DEADLINE = 2;
  public static final int COL_RI_ID = 3;
  public static final int COL_ELEMENT_ID = 4;
  public static final int COL_SEVERITY = 5;
  public static final int COL_SUMMARY = 6;
  public static final String WITH_TASK_SUBQUERY =
      // 1. Preselect RIs for given unit and domain where the control is a leaf (i.e., the control
      // has no parts).
      // 2. From those RIs, produce
      //   2.1. implementation tasks for unimplemented RIs and
      //   2.2. revision tasks for implemented RIs with a revision scheduled.
      """
      with ris as (select ri.*
                   from requirement_implementation as ri
                            inner join element as o on o.db_id = ri.origin_db_id and o.owner_id = :unitId
                            inner join element_domain_association as oda
                                       on oda.owner_db_id = ri.origin_db_id and oda.domain_id = :domainId
                            inner join element_domain_association as cda
                                       on cda.owner_db_id = ri.control_id and cda.domain_id = :domainId
                            left join control_parts as cp on cp.composite_id = ri.control_id
                   group by ri.db_id
                   having count(cp.part_id) = 0),
           t as ((select 'REQUIREMENT_IMPLEMENTATION' as type,
                         ris.person_id                as assignee_id,
                         ris.implementation_until     as deadline,
                         ris.id                       as ri_id,
                         ris.origin_db_id             as element_id,
                         null::severity               as severity,
                         null::text                   as summary
                  from ris
                  where ris.status in ('UNKNOWN', 'NO', 'PARTIAL'))
                 union
                 (select 'REQUIREMENT_IMPLEMENTATION_REVISION' as type,
                         ris.next_revision_by_db_id            as assignee_id,
                         ris.next_revision_date                as deadline,
                         ris.id                                as ri_id,
                         ris.origin_db_id                      as element_id,
                         null::severity                        as severity,
                         null::text                            as summary
                  from ris
                  where ris.status in ('YES', 'N_A')
                    and next_revision_date is not null)
                 union
                 (select 'INSPECTION_FINDING'         as type,
                         if.responsible_person_id     as assignee_id,
                         null                         as deadline,
                         null                         as ri_id,
                         if.element_id                as element_id,
                         if.severity                  as severity,
                         if.task_summary::text        as summary
                  from inspection_finding_data as if
                  where if.domain_id = :domainId
                    and if.unit_id = :unitId))
      """;
  private final EntityManager em;
  private final ElementDataRepository<ElementData> elementRepository;
  private final PersonDataRepository personRepository;
  private final RequirementImplementationDataRepository riRepository;
  private final UserAccessRights accessRights;
  private final Domain domain;
  private final Unit unit;
  private List<String> types = null;
  private List<UUID> assignees = null;

  @Override
  @Transactional(readOnly = true)
  public PagedResult<Task, SortCriterion> execute(PagingConfiguration<SortCriterion> pagingConfig) {
    var totalResultCount = (long) taskQuery(pagingConfig, true).getSingleResult();
    var totalPages = (int) Math.ceilDiv(totalResultCount, pagingConfig.pageSize());
    List<Object[]> resultList = taskQuery(pagingConfig, false).getResultList();

    var risById = getRiMap(resultList);
    var elementsById = getElementMap(resultList, COL_ELEMENT_ID, elementRepository);
    var personsById = getElementMap(resultList, COL_ASSIGNEE_ID, personRepository);
    var pageItems =
        resultList.stream()
            .map(row ->
                new Task(
                    TaskType.valueOf((String) row[COL_TASK_TYPE]),
                    Optional.ofNullable(row[COL_ASSIGNEE_ID])
                        .map(pId -> personsById.get((UUID) pId))
                        .orElse(null),
                    (LocalDate) row[COL_DEADLINE],
                    Optional.ofNullable(row[COL_RI_ID])
                        .map(riId -> risById.get((UUID) riId))
                        .orElse(null),
                    Optional.ofNullable(row[COL_ELEMENT_ID])
                        .map(pId -> elementsById.get((UUID) pId))
                        .orElse(null),
                    Optional.ofNullable(row[COL_SEVERITY])
                        .map(s -> Severity.valueOf((String) s))
                        .orElse(null),
                    Optional.ofNullable(row[COL_SUMMARY])
                        .map(String.class::cast)
                        .map(i -> JsonMapper.shared().readValue(i, TranslatedText.class))
                        .orElse(null)))
            .toList();
    return new PagedResult<>(pagingConfig, pageItems, totalResultCount, totalPages);
  }

  @Override
  public void whereTypeIn(@NonNull Set<TaskType> types) {
    this.types = types.stream().map(TaskType::name).toList();
  }

  @Override
  public void whereAssigneeIn(@NonNull Set<TypedId<Person>> persons) {
    this.assignees = persons.stream().map(TypedId::getId).toList();
  }

  private @NonNull Map<UUID, RequirementImplementation> getRiMap(List<Object[]> resultList) {
    var riIds =
        resultList.stream()
            .map(r -> r[COL_RI_ID])
            .filter(Objects::nonNull)
            .map(UUID.class::cast)
            .collect(Collectors.toSet());
    var riQuery =
        new RequirementImplementationQueryImpl(riRepository, unit.getOwningClient().get());
    riQuery.whereIdsIn(new QueryCondition<>(riIds));
    return riQuery.execute(PagingConfiguration.unpaged("id")).resultPage().stream()
        .collect(Collectors.toMap(RequirementImplementation::getId, Function.identity()));
  }

  private @NonNull <TElement extends ElementData> Map<UUID, TElement> getElementMap(
      List<Object[]> resultList, int idColumn, ElementDataRepository<TElement> repo) {
    var elementIds =
        resultList.stream()
            .map(r -> r[idColumn])
            .filter(Objects::nonNull)
            .map(UUID.class::cast)
            .collect(Collectors.toSet());
    return repo
        .findByIds(
            elementIds,
            accessRights.getClientId(),
            accessRights.isUnitAccessRestricted(),
            accessRights.getReadableUnitIds())
        .stream()
        .collect(Collectors.toMap(Element::getId, Function.identity()));
  }

  @SuppressWarnings("PMD.UseStringBufferForStringAppends") // concatenation count is trivial
  private Query taskQuery(PagingConfiguration<SortCriterion> paging, boolean countOnly) {
    // SELECT (with mandatory filters)
    var params =
        new HashMap<String, Object>(
            Map.of(
                "unitId", unit.getId(),
                "domainId", domain.getId()));
    var query = WITH_TASK_SUBQUERY + (countOnly ? "select count(*) from t\n" : "select * from t\n");

    // WHERE / JOIN (optional filters)
    var conditions = new ArrayList<String>();
    if (types != null) {
      params.put("types", types);
      conditions.add("t.type in :types");
    }
    if (assignees != null) {
      if (assignees.isEmpty()) {
        conditions.add("t.assignee_id is null");
      } else {
        params.put("assignees", assignees);
        conditions.add("t.assignee_id in (:assignees)");
      }
    }
    if (!conditions.isEmpty()) {
      query += "where " + String.join("\n and ", conditions) + "\n";
    }

    // ORDER & LIMIT
    if (!countOnly) {
      var orderCol =
          switch (paging.sortColumn()) {
            case DEADLINE -> "t.deadline";
          };
      params.put("limit", paging.pageSize());
      params.put("offset", paging.pageNumber() * paging.pageSize());
      query +=
          "order by %s %s, severity desc limit :limit offset :offset"
              .formatted(orderCol, paging.sortOrder().getSqlKeyword());
    }

    // EXECUTE
    var queryObj = em.createNativeQuery(query);
    params.forEach(queryObj::setParameter);
    return queryObj;
  }
}
