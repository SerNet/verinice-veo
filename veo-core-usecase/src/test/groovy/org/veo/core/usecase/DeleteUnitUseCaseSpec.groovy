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

import org.veo.core.entity.Unit
import org.veo.core.repository.GenericElementRepository
import org.veo.core.usecase.unit.DeleteUnitUseCase

public class DeleteUnitUseCaseSpec extends UseCaseSpec {
    def "Delete a unit with subunits" () {
        given: "starting values for a unit"
        def uid = UUID.randomUUID()
        Unit existingUnit = Mock(Unit)
        existingUnit.id >> uid
        existingUnit.client >> existingClient
        def genericElementRepository = Mock(GenericElementRepository)

        when: "the unit is deleted"
        def input = new DeleteUnitUseCase.InputData(existingUnit.getId(), existingClient)
        def usecase = new DeleteUnitUseCase(clientRepository, unitRepository, genericElementRepository)
        usecase.execute(input)

        then: "the client for the unit is retrieved"
        2 * clientRepository.getById(_) >> existingClient
        1 * unitRepository.getById(_) >> existingUnit

        1 * genericElementRepository.deleteByUnit(existingUnit)

        and: "the unit is deleted"
        1 * unitRepository.delete(_)
    }
}
