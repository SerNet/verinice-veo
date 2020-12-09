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

import org.veo.core.entity.Key
import org.veo.core.entity.Person
import org.veo.core.entity.transform.TransformTargetToEntityContext
import org.veo.core.usecase.base.GetEntitiesUseCase.InputData
import org.veo.core.usecase.person.GetPersonsUseCase
import org.veo.core.usecase.repository.PersonRepository

class GetPersonsUseCaseSpec extends UseCaseSpec {

    PersonRepository personRepository = Mock()

    GetPersonsUseCase usecase = new GetPersonsUseCase(clientRepository, personRepository, unitHierarchyProvider)

    def "retrieve all persons for a client"() {
        given:

        TransformTargetToEntityContext targetToEntityContext = Mock()
        def id = Key.newUuid()
        Person person = Mock() {
            getOwner() >> existingUnit
            getId() >> id
        }
        when:
        def output = usecase.execute(new InputData(existingClient, Optional.empty(), Optional.empty()))
        then:
        1 * clientRepository.findById(existingClient.id) >> Optional.of(existingClient)
        1 * personRepository.findByClient(existingClient) >> [person]
        output.entities*.id == [id]
    }


    def "retrieve all persons for a unit"() {
        given:
        def id = Key.newUuid()

        Person person = Mock() {
            getOwner() >> existingUnit
            getId() >> id
        }
        when:
        def output = usecase.execute(new InputData(existingClient,
                Optional.of(existingUnit.id.uuidValue()),
                Optional.empty()))
        then:

        1 * clientRepository.findById(existingClient.id) >> Optional.of(existingClient)
        1 * unitHierarchyProvider.findAllInRoot(existingUnit.id) >> existingUnitHierarchyMembers
        1 * personRepository.findByUnits(existingUnitHierarchyMembers) >> [person]
        output.entities*.id == [id]
    }
}