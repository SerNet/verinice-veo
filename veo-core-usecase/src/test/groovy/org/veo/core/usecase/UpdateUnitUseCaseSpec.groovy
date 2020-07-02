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
package org.veo.core.usecase

import org.veo.core.entity.Client
import org.veo.core.entity.Unit
import org.veo.core.entity.specification.ClientBoundaryViolationException
import org.veo.core.entity.transform.TransformTargetToEntityContext
import org.veo.core.usecase.common.NameableInputData
import org.veo.core.usecase.unit.ChangeUnitUseCase
import org.veo.core.usecase.unit.CreateUnitUseCase
import org.veo.core.usecase.unit.CreateUnitUseCase.InputData
import org.veo.core.usecase.unit.UpdateUnitUseCase

public class UpdateUnitUseCaseSpec extends UseCaseSpec {



    def "Update an existing unit" () {
        given: "starting values for a unit"
        def namedInput = new NameableInputData()
        namedInput.setName("New unit")

        and: "a parent unit in an existing client"
        def input = new InputData(namedInput, this.existingClient.getId(), Optional.of(this.existingUnit.getId()))

        and: "fake repositories that record method calls"
        def data2EntityContext = Stub(TransformTargetToEntityContext)

        when: "the use case to create a unit is executed"
        def usecase = new CreateUnitUseCase(clientRepository)
        def newUnit = usecase.execute(input).getUnit()

        and: "the unit is changed and updated"
        newUnit.setName("Name changed")
        def usecase2 = new UpdateUnitUseCase(unitRepository, transformContextProvider)
        def output = usecase2.execute(new ChangeUnitUseCase.InputData(newUnit, this.existingClient))

        then: "a client was retrieved"
        1 * clientRepository.findById(_) >> Optional.of(this.existingClient)

        and: "the client was then saved with the new unit"
        1 * clientRepository.save(_) >> { Client client ->
            assert client.getId() == this.existingClient.getId()
            assert client.getName() == "Existing client"
            client
        }

        and: "a unit was retrieved"
        1 * unitRepository.findById(_) >> Optional.of(this.existingUnit)
        1 * transformContextProvider.createTargetToEntityContext() >> data2EntityContext

        and: "the changed unit was stored"
        1 * unitRepository.save({
            it.name == "Name changed"
        }, _, _) >> { it[0] }
        output.unit != null
        output.unit.name == "Name changed"
        output.unit.id == newUnit.id
    }

    def "Prevent updating a unit from another client" () {
        given: "starting values for a unit"
        def namedInput = new NameableInputData()
        namedInput.setName("New unit")

        and: "a parent unit in an existing client"
        def input = new InputData(namedInput, this.existingClient.getId(), Optional.of(this.existingUnit.getId()))

        and: "a malicious client"
        def  maliciousClient = newClient()

        when: "the use case to create a unit is executed"
        def usecase = new CreateUnitUseCase(clientRepository)
        def newUnit = usecase.execute(input).getUnit()

        and: "the unit is changed and updated by another client"
        newUnit.setName("Name changed")
        def usecase2 = new UpdateUnitUseCase(unitRepository, transformContextProvider)
        def output = usecase2.execute(new ChangeUnitUseCase.InputData(newUnit, maliciousClient))

        then: "a client was retrieved"
        1 * clientRepository.findById(_) >> Optional.of(this.existingClient)

        and: "the client was then saved with the new unit"
        1 * clientRepository.save(_) >> { Client client ->
            client
            assert client.getId() == this.existingClient.getId()
            assert client.getName() == "Existing client"
            client
        }

        and: "a unit was retrieved"
        1 * unitRepository.findById(_) >> Optional.of(this.existingUnit)

        and: "the security violation was prevented"
        thrown ClientBoundaryViolationException
    }
}
