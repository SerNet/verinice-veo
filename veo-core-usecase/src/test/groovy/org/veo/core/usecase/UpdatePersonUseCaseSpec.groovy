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
import org.veo.core.usecase.base.ModifyEntityUseCase.InputData
import org.veo.core.usecase.person.UpdatePersonUseCase
import org.veo.core.usecase.repository.PersonRepository

public class UpdatePersonUseCaseSpec extends UseCaseSpec {

    PersonRepository personRepository = Mock()

    UpdatePersonUseCase usecase = new UpdatePersonUseCase(personRepository)

    def "update a person"() {
        given:
        TransformTargetToEntityContext targetToEntityContext = Mock()
        def id = Key.newUuid()
        Person person = Mock()
        person.id >> id
        person.getOwner() >> existingUnit
        person.name >> "Updated person"

        when:
        def output = usecase.execute(new InputData(person, existingClient))
        then:

        1 * personRepository.save(_) >> person
        output.entity != null
        output.entity.name == "Updated person"
    }
}
