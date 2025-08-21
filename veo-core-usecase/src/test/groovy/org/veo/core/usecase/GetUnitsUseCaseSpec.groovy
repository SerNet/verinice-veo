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

import static java.util.UUID.randomUUID

import org.veo.core.UserAccessRights
import org.veo.core.entity.Client
import org.veo.core.entity.Unit
import org.veo.core.repository.ClientRepository
import org.veo.core.repository.UnitRepository
import org.veo.core.usecase.unit.GetUnitsUseCase
import org.veo.rest.security.NoRestrictionAccessRight

import spock.lang.Specification

public class GetUnitsUseCaseSpec extends Specification {

    Client existingClient
    Unit existingUnit

    def setup() {

        existingClient = Mock()
        existingClient.getId() >> UUID.randomUUID()
        existingClient.getDomains >> []
        existingClient.getName()>> "Existing client"

        existingUnit = Mock()
        existingUnit.getClient() >> existingClient
        existingUnit.getDomains() >> []
        existingUnit.getParent() >> null
        existingUnit.getName() >> "Existing unit"
        existingUnit.getId() >> randomUUID()

        existingClient.getUnits >> [existingUnit]
        existingClient.getUnit(_)>> Optional.of(existingUnit)

        existingClient.createUnit(_)>>existingUnit
    }

    def "Find units by parent unit" () {
        Unit subUnit1 = Mock()
        subUnit1.getDomains() >> []
        subUnit1.getParent() >> existingUnit
        subUnit1.getName() >> "Subunit 1"
        subUnit1.getIdAsString() >> randomUUID()

        Unit subUnit2 = Mock()
        subUnit2.getDomains() >> []
        subUnit2.getParent() >> existingUnit
        subUnit2.getName() >> "Subunit 2"
        subUnit2.getIdAsString() >> randomUUID()

        given: "fake repositories that record method calls"
        def clientRepo = Mock(ClientRepository)
        def unitRepo = Mock(UnitRepository)

        when: "a request is made with a parent-ID"
        def input = new GetUnitsUseCase.InputData(
                Optional.of(existingUnit.id))
        def sot = new GetUnitsUseCase(clientRepo, unitRepo)
        def output = sot.execute(input, NoRestrictionAccessRight.from(existingClient.id.toString()))

        then: "a client was retrieved"
        1 * unitRepo.findById(_) >> Optional.of(existingUnit)
        1 * unitRepo.findByParent(_) >> [subUnit1, subUnit2]

        and: "both subunits are returned"
        output.units.size() == 2
        output.units == [subUnit1, subUnit2]
    }
}
