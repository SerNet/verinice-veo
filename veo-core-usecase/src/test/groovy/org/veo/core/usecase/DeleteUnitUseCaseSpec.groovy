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
import org.veo.core.entity.Control
import org.veo.core.entity.Document
import org.veo.core.entity.Incident
import org.veo.core.entity.Key
import org.veo.core.entity.Person
import org.veo.core.entity.Process
import org.veo.core.entity.Scenario
import org.veo.core.entity.Scope
import org.veo.core.entity.Unit
import org.veo.core.repository.AssetRepository
import org.veo.core.repository.ControlRepository
import org.veo.core.repository.DocumentRepository
import org.veo.core.repository.IncidentRepository
import org.veo.core.repository.PersonRepository
import org.veo.core.repository.ProcessRepository
import org.veo.core.repository.ScenarioRepository
import org.veo.core.repository.ScopeRepository
import org.veo.core.usecase.unit.DeleteUnitUseCase

public class DeleteUnitUseCaseSpec extends UseCaseSpec {
    def "Delete a unit with subunits" () {
        given: "starting values for a unit"
        def uid = Key.newUuid()
        Unit existingUnit = Mock(Unit)
        existingUnit.id >> uid
        existingUnit.client >> existingClient
        Unit subUnit = Mock(Unit)
        def assetRepository = Mock(AssetRepository)
        def controlRepository = Mock(ControlRepository)
        def documentRepository = Mock(DocumentRepository)
        def incidentRepository = Mock(IncidentRepository)
        def personRepository = Mock(PersonRepository)
        def processRepository = Mock(ProcessRepository)
        def scenarioRepository = Mock(ScenarioRepository)
        def scopeRepository = Mock(ScopeRepository)
        when: "the unit is deleted"
        def input = new DeleteUnitUseCase.InputData(existingUnit.getId(), existingClient)
        def usecase = new DeleteUnitUseCase(clientRepository, unitRepository, repositoryProvider)
        usecase.execute(input)

        then: "the client for the unit is retrieved"
        1 * repositoryProvider.getEntityLayerSupertypeRepositoryFor(Asset) >> assetRepository
        1 * repositoryProvider.getEntityLayerSupertypeRepositoryFor(Control) >> controlRepository
        1 * repositoryProvider.getEntityLayerSupertypeRepositoryFor(Document) >> documentRepository
        1 * repositoryProvider.getEntityLayerSupertypeRepositoryFor(Incident) >> incidentRepository
        1 * repositoryProvider.getEntityLayerSupertypeRepositoryFor(Person) >> personRepository
        1 * repositoryProvider.getEntityLayerSupertypeRepositoryFor(Process) >> processRepository
        1 * repositoryProvider.getEntityLayerSupertypeRepositoryFor(Scenario) >> scenarioRepository
        1 * repositoryProvider.getEntityLayerSupertypeRepositoryFor(Scope) >> scopeRepository
        1 * clientRepository.findById(_) >> Optional.of(existingClient)
        1 * unitRepository.findById(_) >> Optional.of(existingUnit)
        1 * scopeRepository.deleteByUnit(existingUnit)
        1 * assetRepository.deleteByUnit(existingUnit)
        1 * controlRepository.deleteByUnit(existingUnit)
        1 * documentRepository.deleteByUnit(existingUnit)
        1 * incidentRepository.deleteByUnit(existingUnit)
        1 * personRepository.deleteByUnit(existingUnit)
        1 * processRepository.deleteByUnit(existingUnit)
        1 * scenarioRepository.deleteByUnit(existingUnit)
        and: "the unit is deleted"
        1 * unitRepository.delete(_)
    }
}
