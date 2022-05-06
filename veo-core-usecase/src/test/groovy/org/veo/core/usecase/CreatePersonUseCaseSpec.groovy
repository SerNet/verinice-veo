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
import org.veo.core.entity.Scope
import org.veo.core.entity.specification.ClientBoundaryViolationException
import org.veo.core.repository.PersonRepository
import org.veo.core.repository.ScopeRepository
import org.veo.core.usecase.base.CreateElementUseCase
import org.veo.core.usecase.decision.Decider
import org.veo.core.usecase.person.CreatePersonUseCase

class CreatePersonUseCaseSpec extends UseCaseSpec {

    ScopeRepository scopeRepository = Mock()
    PersonRepository personRepository = Mock()
    DesignatorService designatorService = Mock()
    Person person = Mock()
    Decider decider = Mock()

    CreatePersonUseCase usecase = new CreatePersonUseCase(unitRepository,scopeRepository, personRepository, designatorService, decider)

    def setup() {
        person.name >> "John"
        person.owner >> existingUnit
        person.links >> []
        person.domains >> []

        unitRepository.findById(_) >> Optional.of(existingUnit)
        scopeRepository.getByIds([] as Set) >> []
    }

    def "create a person"() {
        when:
        def output = usecase.execute(new CreateElementUseCase.InputData(person, existingClient, [] as Set))
        then:
        1 * personRepository.save(person) >> person
        1 * designatorService.assignDesignator(person, existingClient)
        output.entity != null
        output.entity.name == "John"
    }

    def "validates scope client"() {
        given: "a scope for another client"
        def scope = Mock(Scope)
        scope.id >> Key.newUuid()
        scope.checkSameClient(existingClient) >> { throw new ClientBoundaryViolationException(scope, existingClient) }
        scopeRepository.getByIds([scope.id] as Set) >> [scope]

        when: "creating the new process inside the scope"
        usecase.execute(new CreateElementUseCase.InputData(person, existingClient, [scope.id] as Set))

        then: "an exception is thrown"
        thrown(ClientBoundaryViolationException)
    }
}
