/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Jochen Kemnade.
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

import org.veo.core.entity.Key
import org.veo.core.entity.Person
import org.veo.core.repository.PersonRepository
import org.veo.core.usecase.base.ModifyElementUseCase.InputData
import org.veo.core.usecase.common.ETag
import org.veo.core.usecase.decision.Decider
import org.veo.core.usecase.person.UpdatePersonUseCase

public class UpdatePersonUseCaseSpec extends UseCaseSpec {

    PersonRepository personRepository = Mock()
    Decider decider = Mock()
    UpdatePersonUseCase usecase = new UpdatePersonUseCase(personRepository, decider)


    def "update a person"() {
        given:
        def id = Key.newUuid()
        Person person = Mock()
        person.id >> id
        person.getOwner() >> existingUnit
        person.name >> "Updated person"
        person.version >> 0
        person.domains >> []
        person.links >> []

        when:
        def eTag = ETag.from(person.getId().uuidValue(), 0)
        def output = usecase.execute(new InputData(person, existingClient, eTag, "max"))

        then:
        1 * person.version("max", person)
        1 * personRepository.save(_) >> person
        1 * personRepository.findById(_) >> Optional.of(person)
        output.entity != null
        output.entity.name == "Updated person"
    }
}
