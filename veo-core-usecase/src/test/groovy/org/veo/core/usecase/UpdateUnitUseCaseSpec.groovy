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

import org.veo.core.UserAccessRights
import org.veo.core.entity.Client
import org.veo.core.entity.EntityType
import org.veo.core.entity.exception.NotFoundException
import org.veo.core.entity.specification.ClientBoundaryViolationException
import org.veo.core.entity.state.UnitState
import org.veo.core.usecase.common.ETag
import org.veo.core.usecase.service.EntityStateMapper
import org.veo.core.usecase.unit.ChangeUnitUseCase
import org.veo.core.usecase.unit.UnitValidator
import org.veo.core.usecase.unit.UpdateUnitUseCase
import org.veo.rest.security.NoRestrictionAccessRight

public class UpdateUnitUseCaseSpec extends UseCaseSpec {
    static final String USER_NAME = "john"
    UnitValidator unitValidator = Mock()
    UserAccessRights user = Mock()
    EntityStateMapper entityStateMapper = new EntityStateMapper()
    UpdateUnitUseCase updateUseCase = new UpdateUnitUseCase(unitRepository, unitValidator, entityStateMapper, refResolverFactory, clientRepository)

    def "Update an existing unit" () {
        given: "starting values for a unit"
        def newUnit = Mock(UnitState) {
            it.id >> this.existingUnit.idAsString
            it.name >> "Name changed"
            domains >> []
        }

        when: "the use case to create a unit is executed"
        def eTagNewUnit = ETag.from(this.existingUnit.idAsString, 0)
        def output = updateUseCase.execute(new ChangeUnitUseCase.InputData(existingUnit.id, newUnit, eTagNewUnit), noRestrictionExistingClient)

        then: "the existing unit was retrieved"
        1 * unitRepository.getById(existingUnit.id, noRestrictionExistingClient) >> this.existingUnit

        and: "the unit was updated with the changed values"
        1 * existingUnit.setName("Name changed")

        and: "the client was then saved with the new unit"
        1 * unitRepository.save(existingUnit) >> existingUnit
        1 * unitRepository.getById(existingUnit.id, noRestrictionExistingClient) >> existingUnit

        and: "validation has been run"
        1 * unitValidator.validateUpdate(newUnit, existingUnit)

        and: "the changed unit was returned"
        output.unit == existingUnit
    }

    def "Prevent updating a unit from another client" () {
        given: "a malicious client"
        Client maliciousClient = Mock()
        maliciousClient.getId() >> UUID.randomUUID()
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
        updateUseCase.execute(new ChangeUnitUseCase.InputData(existingUnit.id, newUnit, eTag), NoRestrictionAccessRight.from(maliciousClient.id.toString()) )

        then: "the unit was not found"
        unitRepository.getById(existingUnit.id, _) >>{ throw new NotFoundException(existingUnit.id, EntityType.UNIT.type) }

        and: "the security violation was prevented and no information leaked"
        thrown NotFoundException
    }
}
