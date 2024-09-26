/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Jochen Kemnade.
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
package org.veo.persistence.access.jpa;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import org.veo.core.entity.Client;
import org.veo.core.entity.Scope;
import org.veo.core.entity.risk.RiskDefinitionRef;
import org.veo.persistence.entity.jpa.ElementData;
import org.veo.persistence.entity.jpa.ScenarioData;
import org.veo.persistence.entity.jpa.ScopeData;

public interface ScopeDataRepository extends RiskAffectedDataRepository<ScopeData> {

  @Query(
      "select distinct s from scope s "
          + "left join fetch s.risks risks "
          + "left join fetch risks.riskAspects "
          + "where risks.scenario in ?1")
  Set<ScopeData> findRisksWithValue(Collection<ScenarioData> causes);

  @Query(
      "select distinct s from scope s "
          + "left join fetch s.risks risks "
          + "left join fetch risks.riskAspects ra "
          + "left join fetch ra.domain "
          + "where s.dbId IN ?1")
  Set<ScopeData> findByIdsWithRiskValues(Set<UUID> dbIds);

  @Query(
      """
                   select distinct e from #{#entityName} e
                   inner join fetch e.riskValuesAspects
                   inner join fetch e.risks r
                   left join fetch r.riskAspects a
                   left join fetch a.domain
                   inner join fetch r.scenario s
                   left join fetch s.riskValuesAspects
                   where e.dbId in ?1""")
  Set<ScopeData> findWithRisksAndScenariosByDbIdIn(Iterable<UUID> ids);

  @Query(
      """
                     select distinct e from #{#entityName} e
                     inner join fetch e.owner o
                     left join fetch e.riskValuesAspects as rva
                     left join fetch rva.domain as d
                     left join fetch d.riskDefinitionSet
                     inner join fetch e.risks r
                     where o.client = ?1""")
  Set<ScopeData> findAllHavingRisks(Client client);

  <T extends ElementData> Set<Scope> findDistinctByMembersIn(Set<T> elements);

  @Query(
      "select distinct e from #{#entityName} as e "
          + "inner join e.members m "
          + "where m.dbId in ?1 and e.dbId not in ?1")
  Set<Scope> findDistinctOthersByMemberIds(Set<UUID> dbIds);

  @Query(
      "select count(s) > 0 from #{#entityName} as s "
          + "inner join s.scopeRiskValuesAspects r "
          + "inner join s.riskValuesAspects rva "
          + "inner join s.members m "
          + "where m.dbId in ?1 and r.riskDefinitionRef = ?2 and r.domain.dbId = ?3")
  boolean canUseRiskDefinition(
      Set<UUID> elementIds, RiskDefinitionRef riskDefinitionRef, String domainId);

  @Transactional(readOnly = true)
  @EntityGraph(attributePaths = "members")
  List<ScopeData> findAllWithMembersByDbIdIn(List<UUID> ids);

  @Transactional(readOnly = true)
  @EntityGraph(attributePaths = {"riskValuesAspects", "scopeRiskValuesAspects"})
  List<ScopeData> findAllWithRiskValuesAspectsByDbIdIn(List<UUID> ids);

  @Query(
      "select distinct s from scope s "
          + "inner join fetch s.risks risks "
          + "left join fetch risks.riskAspects "
          + "where risks.scenario in ?1")
  List<ScopeData> findRisksWithValue(Set<ScenarioData> scenarios);
}
