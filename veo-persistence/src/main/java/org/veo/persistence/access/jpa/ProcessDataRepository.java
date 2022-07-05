/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2019  Urs Zeidler.
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

import javax.annotation.Nonnull;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;

import org.veo.core.entity.Client;
import org.veo.persistence.entity.jpa.ProcessData;
import org.veo.persistence.entity.jpa.ScenarioData;

public interface ProcessDataRepository extends CompositeRiskAffectedDataRepository<ProcessData> {
  @Query(
      "select distinct p from process p "
          + "left join fetch p.risks risks "
          + "left join fetch risks.riskAspects "
          + "where risks.scenario in ?1")
  Set<ProcessData> findRisksWithValue(Collection<ScenarioData> causes);

  @Query(
      "select distinct p from process p "
          + "left join fetch p.risks risks "
          + "left join fetch risks.riskAspects ra "
          + "left join fetch ra.domain "
          + "where p.dbId IN ?1")
  Set<ProcessData> findByIdsWithRiskValues(Set<String> dbIds);

  @Query(
      "select distinct e from #{#entityName} e "
          + "left join fetch e.owner o "
          + "left join fetch e.riskValuesAspects "
          + "inner join fetch e.risks r "
          + "left join fetch r.riskAspects "
          + "inner join fetch r.domains "
          + "inner join fetch r.scenario s "
          + "left join fetch s.riskValuesAspects "
          + " where o.client = ?1")
  Set<ProcessData> findAllHavingRisks(Client client);

  @Nonnull
  @EntityGraph(ProcessData.AGGREGATE_GRAPH_WITH_RISKS)
  List<ProcessData> findAllWithRisksByDbIdIn(Iterable<String> ids);
}
