/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Alexander Koderman.
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
package org.veo.core.usecase

import org.veo.core.entity.Asset
import org.veo.core.entity.Process
import org.veo.core.entity.Unit
import org.veo.core.repository.AssetRepository
import org.veo.core.repository.ProcessRepository
import org.veo.core.service.DomainTemplateService
import org.veo.core.usecase.unit.CreateDemoUnitUseCase
import org.veo.core.usecase.unit.CreateDemoUnitUseCase.InputData

public class CreateDemoUnitUseCaseSpec extends UseCaseSpec {

    DomainTemplateService domainTemplateService = Mock()
    AssetRepository assetRepository = Mock()
    ProcessRepository processRepository = Mock()

    CreateDemoUnitUseCase usecase = new CreateDemoUnitUseCase(clientRepository, unitRepository, entityFactory, domainTemplateService,repositoryProvider)

    def "Create a new demo unit for an existing client" () {
        given: "starting values for a unit"

        Unit demoUnit = Mock()

        Asset asset1 = Mock {
            getModelInterface() >> Asset
            getParts() >> []
        }
        Asset asset2 = Mock() {
            getModelInterface() >> Asset
            getParts() >> []
        }
        Process process = Mock {
            getModelInterface() >> Process
            getParts() >> []
        }


        and: "a parent unit in an existing client"
        def input = new InputData(this.existingClient.getId())

        when: "the use case to create a unit is executed"
        def newUnit = usecase.execute(input).getUnit()

        then: "a client is retrieved"
        1 * clientRepository.findById(_) >> Optional.of(existingClient)

        and: "a new unit is created in the client"
        1 * entityFactory.createUnit("Demo", null) >> demoUnit
        1 * demoUnit.setClient(existingClient)

        and: "the demo unit elements are created for the unit"
        1 * domainTemplateService.getElementsForDemoUnit(existingClient) >> [asset1, asset2, process]

        with(asset1) {
            1 * setDesignator('DMO-1')
            1 * setOwner(demoUnit)
        }
        with(asset2) {
            1 * setDesignator('DMO-2')
            1 * setOwner(demoUnit)
        }
        with(process) {
            1 * setDesignator('DMO-3')
            1 * setOwner(demoUnit)
        }

        and: "everything is saved in the database"
        1 * unitRepository.save(_) >> demoUnit
        1 * repositoryProvider.getElementRepositoryFor(Asset) >> assetRepository
        1 * repositoryProvider.getElementRepositoryFor(Process) >> processRepository
        1 * assetRepository.saveAll([asset1, asset2] as Set)
        1 * processRepository.saveAll([process] as Set)

        and: "a new unit was created and stored"
        newUnit != null
    }
}
