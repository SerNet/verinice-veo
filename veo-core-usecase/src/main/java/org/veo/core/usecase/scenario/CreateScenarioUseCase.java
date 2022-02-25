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
package org.veo.core.usecase.scenario;

import org.veo.core.entity.Scenario;
import org.veo.core.repository.ScenarioRepository;
import org.veo.core.repository.UnitRepository;
import org.veo.core.usecase.DesignatorService;
import org.veo.core.usecase.base.CreateElementUseCase;

public class CreateScenarioUseCase extends CreateElementUseCase<Scenario> {

    public CreateScenarioUseCase(UnitRepository unitRepository, ScenarioRepository entityRepo,
            DesignatorService designatorService) {
        super(unitRepository, entityRepo, designatorService);
    }

    @Override
    protected void validate(Scenario scenario) {
        // GNDN
    }
}
