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
import org.veo.core.entity.specification.ClientBoundaryViolationException
import org.veo.core.entity.state.UnitState
import org.veo.core.usecase.common.ETag
import org.veo.core.usecase.service.EntityStateMapper
import org.veo.core.usecase.unit.ChangeUnitUseCase
import org.veo.core.usecase.unit.UnitValidator
import org.veo.core.usecase.unit.UpdateUnitUseCase

public class UpdateUnitUseCaseSpec extends UseCaseSpec {
    static final String USER_NAME = "john"
    UnitValidator unitValidator = Mock()
    EntityStateMapper entityStateMapper = new EntityStateMapper()
    UpdateUnitUseCase updateUseCase = new UpdateUnitUseCase(unitRepository, unitValidator, entityStateMapper, refResolverFactory)

    def "Update an existing unit" () {
        given: "starting values for a unit"
        def newUnit = Mock(UnitState) {
            it.id >> this.existingUnit.idAsString
            it.name >> "Name changed"
            domains >> []
        }

        when: "the use case to create a unit is executed"
        def eTagNewUnit = ETag.from(this.existingUnit.idAsString, 0)
        def output = updateUseCase.execute(new ChangeUnitUseCase.InputData(existingUnit.idAsUUID, newUnit, this.existingClient, eTagNewUnit, USER_NAME))

        then: "the existing unit was retrieved"
        1 * unitRepository.getById(existingUnit.id) >> this.existingUnit

        and: "client boundaries were validated"
        1 * this.existingUnit.checkSameClient(existingClient)

        and: "the unit was updated with the changed values"
        1 * existingUnit.setName("Name changed")

        and: "the client was then saved with the new unit"
        1 * unitRepository.save(existingUnit) >> existingUnit
        1 * unitRepository.getById(existingUnit.id) >> existingUnit

        and: "validation has been run"
        1 * unitValidator.validateUpdate(newUnit, existingUnit)

        and: "the changed unit was returned"
        output.unit == existingUnit
    }

    def "Prevent updating a unit from another client" () {
        given: "a malicious client"
        Client maliciousClient = Mock()
        maliciousClient.getId() >> Key.newUuid()
        maliciousClient.getDomains >> []
        maliciousClient.getName() >> "Existing client"

        when: "the use case to create a unit is executed"
        def newUnit = Mock(UnitState) {
            it.id >> this.existingUnit.idAsString
            it.client >> existingClient
        }

        and: "the unit is changed and updated by another client"
        newUnit.setName("Name changed")
        def eTag = ETag.from(existingUnit.idAsString, existingUnit.getVersion())
        updateUseCase.execute(new ChangeUnitUseCase.InputData(existingUnit.idAsUUID, newUnit, maliciousClient, eTag, USER_NAME))

        then: "a unit was retrieved"
        unitRepository.getById(_) >> this.existingUnit
        existingUnit.checkSameClient(_) >> { throw new ClientBoundaryViolationException(existingUnit, maliciousClient) }

        and: "the security violation was prevented"
        thrown ClientBoundaryViolationException
    }
}
