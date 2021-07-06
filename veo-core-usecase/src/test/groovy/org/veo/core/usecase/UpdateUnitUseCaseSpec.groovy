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


import org.veo.core.entity.Client
import org.veo.core.entity.Key
import org.veo.core.entity.Unit
import org.veo.core.entity.specification.ClientBoundaryViolationException
import org.veo.core.usecase.common.ETag
import org.veo.core.usecase.unit.ChangeUnitUseCase
import org.veo.core.usecase.unit.UpdateUnitUseCase

public class UpdateUnitUseCaseSpec extends UseCaseSpec {
    static final String USER_NAME = "john"
    UpdateUnitUseCase updateUseCase = new UpdateUnitUseCase(unitRepository)

    def "Update an existing unit" () {
        given: "starting values for a unit"
        def newUnit = Mock(Unit) {
            it.id >> this.existingUnit.id
            it.name >> "Name changed"
            it.client >> this.existingClient
        }

        when: "the use case to create a unit is executed"

        def eTagNewUnit = ETag.from(this.existingUnit.getId().uuidValue(), 0)
        def output = updateUseCase.execute(new ChangeUnitUseCase.InputData(newUnit, this.existingClient, eTagNewUnit, USER_NAME))

        then: "the existing unit was retrieved"
        1 * unitRepository.findById(_) >> Optional.of(this.existingUnit)

        and: "client boundaries were validated"
        1 * this.existingUnit.checkSameClient(existingClient)
        1 * newUnit.checkSameClient(existingClient)

        and: "the client was then saved with the new unit"
        1 * newUnit.version(USER_NAME, this.existingUnit)
        1 * unitRepository.save(_) >> newUnit

        and: "the changed unit was returned"
        output.unit == newUnit
    }

    def "Prevent updating a unit from another client" () {
        given: "a malicious client"
        Client maliciousClient = Mock()
        maliciousClient.getId() >> Key.newUuid()
        maliciousClient.getDomains >> []
        maliciousClient.getName() >> "Existing client"

        when: "the use case to create a unit is executed"
        def newUnit = Mock(Unit) {
            it.id >> this.existingClient.id
            it.client >> existingClient
        }

        and: "the unit is changed and updated by another client"
        newUnit.setName("Name changed")
        def eTag = ETag.from(newUnit.getId().uuidValue(), newUnit.getVersion())
        updateUseCase.execute(new ChangeUnitUseCase.InputData(newUnit, maliciousClient, eTag, USER_NAME))

        then: "a unit was retrieved"
        unitRepository.findById(_) >> Optional.of(this.existingUnit)
        existingUnit.checkSameClient(_) >> { throw new ClientBoundaryViolationException(existingUnit, maliciousClient) }

        and: "the security violation was prevented"
        thrown ClientBoundaryViolationException
    }
}
