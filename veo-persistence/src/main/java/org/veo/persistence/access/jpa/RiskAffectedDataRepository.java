/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Finn Westendorf.
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

import jakarta.annotation.Nonnull;

import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import org.veo.persistence.entity.jpa.ControlData;
import org.veo.persistence.entity.jpa.PersonData;
import org.veo.persistence.entity.jpa.RiskAffectedData;
import org.veo.persistence.entity.jpa.ScenarioData;

@Transactional(readOnly = true)
public interface RiskAffectedDataRepository<T extends RiskAffectedData<?, ?>>
    extends ElementDataRepository<T> {

  @SuppressWarnings("PMD.MethodNamingConventions")
  Set<T> findDistinctByRisks_ScenarioIn(Collection<ScenarioData> causes);

  @SuppressWarnings("PMD.MethodNamingConventions")
  Set<T> findDistinctByRisks_Mitigation_In(Collection<ControlData> controls);

  @SuppressWarnings("PMD.MethodNamingConventions")
  Set<T> findDistinctByRisks_RiskOwner_In(Collection<PersonData> persons);

  @Nonnull
  @Query(
      """
         select distinct e from #{#entityName} e
         left join fetch e.risks r
         left join fetch r.scenario
         left join fetch r.mitigation
         left join fetch r.riskOwner
         left join fetch r.riskAspects a
         left join fetch a.domain
         where e.dbId in ?1""")
  List<T> findAllWithRisksByDbIdIn(Iterable<UUID> ids);

  @Nonnull
  @Query(
      """
        select distinct e from #{#entityName} e
        left join fetch e.controlImplementations
        where e.dbId in ?1
        """)
  Set<T> findAllWithCIs(Iterable<UUID> ids);

  @Nonnull
  @Query(
      """
        select distinct e from #{#entityName} e
        left join fetch e.requirementImplementations
        where e.dbId in ?1
        """)
  Set<T> findAllWithRIs(Iterable<UUID> ids);

  // RIs must be joined twice, because the non-matching RIs would be missing from the returned
  // elements if the join-fetched RIs were used in the `where` clause.
  // https://stackoverflow.com/questions/5816417/how-to-properly-express-jpql-join-fetch-with-where-clause-as-jpa-2-criteriaq
  @Query(
      """
        select distinct e from #{#entityName} e
        left join fetch e.controlImplementations
        left join fetch e.requirementImplementations
        left join e.requirementImplementations ri
        where ri.control in ?1
        """)
  Set<T> findAllByRequirementImplementationControls(Iterable<ControlData> controls);
}
