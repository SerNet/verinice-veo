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
package org.veo.core.usecase

import org.veo.core.entity.Control
import org.veo.core.entity.Key
import org.veo.core.entity.transform.TransformTargetToEntityContext
import org.veo.core.usecase.base.GetEntitiesUseCase.InputData
import org.veo.core.usecase.control.GetControlsUseCase
import org.veo.core.usecase.repository.ControlRepository

class GetControlsUseCaseSpec extends UseCaseSpec {

    ControlRepository controlRepository = Mock()

    GetControlsUseCase usecase = new GetControlsUseCase(clientRepository, controlRepository, unitHierarchyProvider)

    def "retrieve all controls for a client"() {
        given:
        TransformTargetToEntityContext targetToEntityContext = Mock()
        def id = Key.newUuid()
        Control control = Mock() {
            getOwner() >> existingUnit
            getId() >> id
        }
        when:
        def output = usecase.execute(new InputData(existingClient, Optional.empty(), Optional.empty()))
        then:
        1 * clientRepository.findById(existingClient.id) >> Optional.of(existingClient)
        1 * controlRepository.findByClient(existingClient, false) >> [control]
        output.entities*.id == [id]
    }


    def "retrieve all controls for a unit"() {
        given:
        TransformTargetToEntityContext targetToEntityContext = Mock()
        def id = Key.newUuid()
        Control control = Mock() {
            getOwner() >> existingUnit
            getId() >> id
        }
        when:
        def output = usecase.execute(
                new InputData(existingClient, Optional.of(existingUnit.id.uuidValue()),
                Optional.empty()
                )
                )
        then:
        1 * clientRepository.findById(existingClient.id) >> Optional.of(existingClient)
        1 * unitHierarchyProvider.findAllInRoot(existingUnit.id) >> existingUnitHierarchyMembers
        1 * controlRepository.findByUnits(existingUnitHierarchyMembers) >> [control]
        output.entities*.id == [id]
    }
}