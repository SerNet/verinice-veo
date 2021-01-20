/*******************************************************************************
 * Copyright (c) 2020 Alexander Koderman.
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
package org.veo.core.usecase.asset;

import javax.transaction.Transactional;

import org.veo.core.usecase.repository.AssetRepository;
import org.veo.core.usecase.repository.ControlRepository;
import org.veo.core.usecase.repository.DomainRepository;
import org.veo.core.usecase.repository.PersonRepository;
import org.veo.core.usecase.repository.ScenarioRepository;

public class CreateAssetRiskUseCase extends AssetRiskUseCase {

    public CreateAssetRiskUseCase(AssetRepository assetRepository,
            ScenarioRepository scenarioRepository, ControlRepository controlRepository,
            PersonRepository personRepository, DomainRepository domainRepository) {
        super(assetRepository, controlRepository, personRepository, scenarioRepository,
                domainRepository);
    }

    @Transactional
    @Override
    public OutputData execute(InputData input) {
        // Retrieve the necessary entities for the requested operation:
        var asset = assetRepository.findById(input.getAssetRef())
                                   .orElseThrow();

        var scenario = scenarioRepository.findById(input.getScenarioRef())
                                         .orElseThrow();

        var domains = domainRepository.getByIds(input.getDomainRefs());

        // Validate security constraints:
        asset.checkSameClient(input.getAuthenticatedClient());
        scenario.checkSameClient(input.getAuthenticatedClient());
        checkClients(input.getAuthenticatedClient(), domains);

        // Apply requested operation:
        var risk = asset.newRisk(scenario, domains);

        risk = applyOptionalInput(input, risk);

        return new OutputData(risk);
    }

}
