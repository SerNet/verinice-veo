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

import org.veo.core.entity.Key
import org.veo.core.entity.Unit
import org.veo.core.usecase.common.NameableInputData
import org.veo.core.usecase.unit.CreateUnitUseCase
import org.veo.core.usecase.unit.CreateUnitUseCase.InputData

public class CreateUnitUseCaseSpec extends UseCaseSpec {
    CreateUnitUseCase usecase = new CreateUnitUseCase(clientRepository, unitRepository, entityFactory)

    def "Create new unit in a new client" () {
        Unit newUnit1 = Mock()
        newUnit1.id >> Key.newUuid()
        newUnit1.name >> "New unit"

        given: "starting values for a unit"
        def namedInput = new NameableInputData()
        namedInput.setName("New unit")

        and: "a clientId that does not yet exist"
        def newClientId = Key.newUuid()
        def input = new InputData(namedInput, newClientId, Optional.empty())

        when: "the use case to create a unit is executed"
        def newUnit = usecase.execute(input).getUnit()

        then: "a client was first searched but not found"
        1 * clientRepository.findById(_) >> Optional.empty()
        1 * entityFactory.createClient(_,_) >> existingClient
        1 * entityFactory.createUnit(_,_) >> newUnit1
        1 * entityFactory.createDomain(_,_,_,_) >> existingDomain

        and: "a new client was then correctly created and stored"
        1 * unitRepository.save(_) >> newUnit1
        1 * clientRepository.save(_) >> existingClient

        and: "a new unit was created and stored"
        newUnit != null
        newUnit.getName() == "New unit"
        !newUnit.getId().isUndefined()
    }

    def "Create a new subunit in an existing unit" () {
        given: "starting values for a unit"
        def namedInput = new NameableInputData()
        namedInput.setName("New unit")

        Unit newUnit1 = Mock()
        newUnit1.id >> Key.newUuid()
        newUnit1.name >> "New unit"
        newUnit1.parent >> existingUnit

        entityFactory.createUnit(_,_) >> newUnit1

        and: "a parent unit in an existing client"
        def input = new InputData(namedInput, this.existingClient.getId(), Optional.of(this.existingUnit.getId()))

        when: "the use case to create a unit is executed"
        def newUnit = usecase.execute(input).getUnit()

        then: "a client was retrieved"
        1 * clientRepository.findById(_) >> Optional.of(this.existingClient)

        and: "a new client was then correctly created and stored"
        1 * unitRepository.save(_) >> newUnit1
        1 * unitRepository.findById(_) >> Optional.of(existingUnit)

        and: "a new unit was created and stored"
        newUnit != null
        newUnit.getName() == "New unit"
        !newUnit.getId().isUndefined()
        newUnit.getParent().getId() == existingUnit.getId()
    }
}
