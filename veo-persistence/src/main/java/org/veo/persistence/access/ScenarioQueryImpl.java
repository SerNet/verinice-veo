/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Jochen Kemnade
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

import java.util.List;

import org.veo.core.entity.Client;
import org.veo.core.entity.Scenario;
import org.veo.persistence.access.jpa.ScenarioDataRepository;
import org.veo.persistence.entity.jpa.ScenarioData;

public class ScenarioQueryImpl extends CompositeElementQueryImpl<Scenario, ScenarioData> {

  private final ScenarioDataRepository scenarioRepository;

  public ScenarioQueryImpl(ScenarioDataRepository repo, Client client) {
    super(repo, client);
    this.scenarioRepository = repo;
  }

  @Override
  protected List<ScenarioData> fullyLoadItems(List<String> ids) {
    List<ScenarioData> result = super.fullyLoadItems(ids);

    if (fetchRiskValuesAspects) {
      scenarioRepository.findAllWithRiskValuesAspectsByDbIdIn(ids);
    }
    return result;
  }
}
