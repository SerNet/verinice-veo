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

import org.veo.core.entity.transform.TransformTargetToEntityContext
import org.veo.core.usecase.person.CreatePersonUseCase
import org.veo.core.usecase.person.CreatePersonUseCase.InputData
import org.veo.core.usecase.repository.PersonRepository

public class CreatePersonUseCaseSpec extends UseCaseSpec {

    PersonRepository personRepository = Mock()

    CreatePersonUseCase usecase = new CreatePersonUseCase(unitRepository, personRepository, transformContextProvider)

    def "create a person"() {
        given:
        TransformTargetToEntityContext targetToEntityContext = Mock()

        when:
        def output = usecase.execute(new InputData(existingUnit.id, "John", existingClient))
        then:
        1 * transformContextProvider.createTargetToEntityContext() >> targetToEntityContext
        1 * targetToEntityContext.partialClient() >> targetToEntityContext
        1 * unitRepository.findById(_, _) >> Optional.of(existingUnit)
        1 * personRepository.save({
            it.name == "John"
        }) >> { it[0] }
        output.person != null
        output.person.name == "John"
    }
}
