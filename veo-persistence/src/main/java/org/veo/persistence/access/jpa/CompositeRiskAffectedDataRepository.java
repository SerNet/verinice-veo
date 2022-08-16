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

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.transaction.annotation.Transactional;

import org.veo.persistence.entity.jpa.ControlData;
import org.veo.persistence.entity.jpa.PersonData;
import org.veo.persistence.entity.jpa.RiskAffectedData;
import org.veo.persistence.entity.jpa.ScenarioData;

@Transactional(readOnly = true)
@NoRepositoryBean
public interface CompositeRiskAffectedDataRepository<T extends RiskAffectedData<?, ?>>
    extends CompositeEntityDataRepository<T> {

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
         left join fetch r.domains
         left join fetch r.scenario
         left join fetch r.mitigation
         left join fetch r.riskOwner
         left join fetch r.riskAspects
         where e.dbId in ?1""")
  List<T> findAllWithRisksByDbIdIn(Iterable<String> ids);
}
