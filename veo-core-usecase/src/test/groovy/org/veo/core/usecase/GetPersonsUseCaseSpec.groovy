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
import org.veo.core.entity.transform.EntityFactory
import org.veo.core.entity.transform.TransformTargetToEntityContext
import org.veo.core.usecase.base.GetEntitiesUseCase.InputData
import org.veo.core.usecase.person.GetPersonsUseCase
import org.veo.core.usecase.repository.PersonRepository
import org.veo.core.usecase.repository.UnitRepository

class GetPersonsUseCaseSpec extends UseCaseSpec {

    PersonRepository personRepository = Mock()
    UnitRepository unitRepository = Mock()

    GetPersonsUseCase usecase = new GetPersonsUseCase(clientRepository, personRepository, unitRepository)

    def "retrieve all persons for a client"() {
        given:
        EntityFactory factory = Mock()
        factory.createPerson()>> Mock(Person.class)

        TransformTargetToEntityContext targetToEntityContext = Mock()
        targetToEntityContext.entityFactory >> factory
        def id = Key.newUuid()
        Person person = Mock() {
            getOwner() >> existingUnit
            getId() >> id
        }
        when:
        def output = usecase.execute(new InputData(existingClient, Optional.empty()))
        then:
        1 * clientRepository.findById(existingClient.id) >> Optional.of(existingClient)
        1 * personRepository.findByClient(existingClient, false) >> [person]
        output.entities*.id == [id]
    }


    def "retrieve all persons for a unit"() {
        given:
        def id = Key.newUuid()
        Person p = Mock()
        p.getId() >> id
        p.getOwner >> existingUnit

        EntityFactory factory = Mock()
        factory.createPerson()>> p

        TransformTargetToEntityContext targetToEntityContext = Mock()
        targetToEntityContext.entityFactory >> factory
        Person person = Mock() {
            getOwner() >> existingUnit
            getId() >> id
        }
        when:
        def output = usecase.execute(new InputData(existingClient, Optional.of(existingUnit.id.uuidValue())))
        then:

        1 * clientRepository.findById(existingClient.id) >> Optional.of(existingClient)
        1 * unitRepository.findById(existingUnit.id) >> Optional.of(existingUnit)
        1 * personRepository.findByUnit(existingUnit, false) >> [person]
        output.entities*.id == [id]
    }
}