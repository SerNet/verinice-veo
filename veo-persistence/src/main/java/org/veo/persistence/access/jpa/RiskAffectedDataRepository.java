/*******************************************************************************
 * Copyright (c) 2019 Urs Zeidler.
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.veo.persistence.access.jpa;

import java.util.Set;

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.transaction.annotation.Transactional;

import org.veo.persistence.entity.jpa.ControlData;
import org.veo.persistence.entity.jpa.PersonData;
import org.veo.persistence.entity.jpa.RiskAffectedData;
import org.veo.persistence.entity.jpa.ScenarioData;

@Transactional(readOnly = true)
@NoRepositoryBean
public interface RiskAffectedDataRepository<T extends RiskAffectedData<?, ?>>
        extends CompositeEntityDataRepository<T> {

    Set<T> findDistinctByRisks_ScenarioIn(Set<ScenarioData> causes);

    Set<T> findDistinctByRisks_Mitigation_In(Set<ControlData> controls);

    Set<T> findDistinctByRisks_RiskOwner_In(Set<PersonData> persons);
}
