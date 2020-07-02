/*******************************************************************************
 * Copyright (c) 2020 Jochen Kemnade.
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
package org.veo.core.usecase.process

import org.veo.core.entity.Key
import org.veo.core.entity.Process
import org.veo.core.entity.transform.TransformTargetToEntityContext
import org.veo.core.usecase.UseCaseSpec
import org.veo.core.usecase.base.GetEntitiesUseCase.InputData
import org.veo.core.usecase.repository.ProcessRepository

class GetProcessesUseCaseSpec extends UseCaseSpec {

    ProcessRepository processRepository = Mock()

    GetProcessesUseCase usecase = new GetProcessesUseCase(clientRepository, processRepository)

    def "retrieve all processes for a client"() {
        given:
        def id = Key.newUuid()
        Process process = Mock() {
            getOwner() >> existingUnit
            getId() >> id
        }
        when:
        def entities = usecase.execute(new InputData(existingClient, Optional.empty()))
        then:
        1 * clientRepository.findById(existingClient.id) >> Optional.of(existingClient)
        1 * processRepository.findByClient(existingClient, false) >> [process]
        entities*.id == [id]
    }


    def "retrieve all processes for a unit"() {
        given:
        def id = Key.newUuid()
        Process process = Mock() {
            getOwner() >> existingUnit
            getId() >> id
        }
        when:
        def entities = usecase.execute(new InputData(existingClient, Optional.of(existingUnit.id.uuidValue())))
        then:
        1 * clientRepository.findById(existingClient.id) >> Optional.of(existingClient)
        1 * processRepository.findByUnit(existingUnit, false) >> [process]
        entities*.id == [id]
    }
}