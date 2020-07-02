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
import org.veo.core.entity.Key
import org.veo.core.usecase.common.NameableInputData
import org.veo.core.usecase.unit.CreateUnitUseCase
import org.veo.core.usecase.unit.CreateUnitUseCase.InputData

public class CreateUnitUseCaseSpec extends UseCaseSpec {


    def "Create new unit in a new client" () {
        given: "starting values for a unit"
        def namedInput = new NameableInputData()
        namedInput.setName("New unit")

        and: "a clientId that does not yet exist"
        def newClientId = Key.newUuid()
        def input = new InputData(namedInput, newClientId, Optional.empty())

        when: "the use case to create a unit is executed"
        def usecase = new CreateUnitUseCase(clientRepository)
        def newUnit = usecase.execute(input).getUnit()

        then: "a client was first searched but not found"
        1 * clientRepository.findById(_) >> Optional.empty()

        and: "a new client was then correctly created and stored"
        1 * clientRepository.save(_) >> { Client client ->
            client
            assert client.getId() == newClientId
            assert client.getName() == "New unit" // Note: client gets the unit's name by default
            client
        }

        and: "a new unit was created and stored"
        newUnit != null
        newUnit.getName() == "New unit"
        !newUnit.getId().isUndefined()
    }

    def "Create a new subunit in an existing unit" () {
        given: "starting values for a unit"
        def namedInput = new NameableInputData()
        namedInput.setName("New unit")

        and: "a parent unit in an existing client"
        def input = new InputData(namedInput, this.existingClient.getId(), Optional.of(this.existingUnit.getId()))

        when: "the use case to create a unit is executed"
        def usecase = new CreateUnitUseCase(clientRepository)
        def newUnit = usecase.execute(input).getUnit()

        then: "a client was retrieved"
        1 * clientRepository.findById(_) >> Optional.of(this.existingClient)

        and: "a new client was then correctly created and stored"
        1 * clientRepository.save(_) >> { Client client ->
            client
            assert client.getId() == this.existingClient.getId()
            assert client.getName() == "Existing client"
            client
        }

        and: "a new unit was created and stored"
        newUnit != null
        newUnit.getName() == "New unit"
        !newUnit.getId().isUndefined()
        newUnit.getParent().getId() == existingUnit.getId()
    }
}
