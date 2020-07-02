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
import org.veo.core.entity.Unit
import org.veo.core.entity.impl.ClientImpl
import org.veo.core.entity.specification.ClientBoundaryViolationException
import org.veo.core.entity.transform.TransformContextProvider
import org.veo.core.entity.transform.TransformTargetToEntityContext
import org.veo.core.usecase.common.NameableInputData
import org.veo.core.usecase.repository.ClientRepository
import org.veo.core.usecase.repository.UnitRepository
import org.veo.core.usecase.unit.ChangeUnitUseCase
import org.veo.core.usecase.unit.CreateUnitUseCase
import org.veo.core.usecase.unit.CreateUnitUseCase.InputData
import org.veo.core.usecase.unit.GetUnitsUseCase
import org.veo.core.usecase.unit.UpdateUnitUseCase
import spock.lang.Specification

public class GetUnitsUseCaseSpec extends Specification {


    Client existingClient
    Unit existingUnit

    def setup() {
        existingClient = new ClientImpl(Key.newUuid(), "Existing client")
        existingUnit = existingClient.createUnit("Existing unit")
        def subUnit1 = existingUnit.createSubUnit("Subunit 1")
        def subUnit2 = existingUnit.createSubUnit("Subunit 2")
    }

    def "Find units by parent unit" () {
        given: "fake repositories that record method calls"
        def clientRepo = Mock(ClientRepository)

        when: "a request is made with a parent-ID"
        def input = new GetUnitsUseCase.InputData(existingClient,
                Optional.of(existingUnit.getId().uuidValue()))
        def sot = new GetUnitsUseCase(clientRepo, null)
        def output = sot.execute(input)

        then: "the client was retrieved"
        then: "a client was retrieved"
        1 * clientRepo.findById(_) >> Optional.of(this.existingClient)

        and: "both subunits are returned"
        output.units.size() == 2
        output.units == existingUnit.units
    }
}
