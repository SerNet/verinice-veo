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
import org.veo.core.usecase.person.GetPersonUseCase
import org.veo.core.usecase.repository.PersonRepository

class GetPersonUseCaseSpec extends UseCaseSpec {

    PersonRepository personRepository = Mock()

    GetPersonUseCase usecase = new GetPersonUseCase(personRepository)

    def "retrieve a person"() {
        given:

        def id = Key.newUuid()
        Person person = Mock() {
            getOwner() >> existingUnit
            getId() >> id
        }
        when:
        def output = usecase.execute(new UseCase.IdAndClient(id,  existingClient))
        then:
        1 * personRepository.findById(id) >> Optional.of(person)
        output.person != null
        output.person.id == id
    }
}