/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Alexander Ben Nasrallah.
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

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import org.veo.core.entity.Domain;
import org.veo.persistence.entity.jpa.ScenarioData;

public interface ScenarioDataRepository extends CompositeEntityDataRepository<ScenarioData> {

  @Transactional(readOnly = true)
  @EntityGraph(attributePaths = "riskValuesAspects")
  List<ScenarioData> findAllWithRiskValuesAspectsByIdIn(List<UUID> ids);

  @Query("SELECT e FROM #{#entityName} as e RIGHT JOIN FETCH e.riskValuesAspects")
  Set<ScenarioData> findByDomainWhereRiskValuesExist(Domain domain);
}
