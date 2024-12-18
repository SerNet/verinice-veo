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

import org.veo.core.entity.Person
import org.veo.core.entity.Scope
import org.veo.core.entity.Unit
import org.veo.core.entity.ref.TypedId
import org.veo.core.entity.specification.ClientBoundaryViolationException
import org.veo.core.entity.state.ElementState
import org.veo.core.repository.PersonRepository
import org.veo.core.repository.ScopeRepository
import org.veo.core.service.EventPublisher
import org.veo.core.usecase.base.CreateElementUseCase
import org.veo.core.usecase.decision.Decider
import org.veo.core.usecase.service.EntityStateMapper

class CreatePersonUseCaseSpec extends UseCaseSpec {
    DesignatorService designatorService = Mock()
    EventPublisher eventPublisher = Mock()

    EntityStateMapper entityStateMapper = Mock()
    Decider decider = Mock()
    ScopeRepository scopeRepository = Mock()
    PersonRepository personRepository = Mock()
    ElementState personState
    Person person = Mock {
        links >> []
        scopes >> []
        parts >> []
        composites >> []
        appliedCatalogItems >> []
    }

    CreateElementUseCase usecase = new CreateElementUseCase(refResolverFactory, repositoryProvider, designatorService, eventPublisher, identifiableFactory, entityStateMapper, decider)

    def setup() {
        personState = Mock() {
            modelInterface >> Person
            name >> "John"
            owner >> TypedId.from(existingUnit.id, Unit)
        }

        repositoryProvider.getElementRepositoryFor(Scope) >> scopeRepository
        repositoryProvider.getElementRepositoryFor(Person) >> personRepository
        scopeRepository.findByIds([] as Set) >> []
    }

    def "create a person"() {
        when:
        def output = usecase.execute(new CreateElementUseCase.InputData(personState, existingClient, [] as Set))

        then:
        1 * identifiableFactory.create(Person) >> person
        1 * entityStateMapper.mapState(personState, person, false, _)
        1 * personRepository.save(person) >> person
        1 * designatorService.assignDesignator(person, existingClient)
        1 * person.getOwner() >> existingUnit
        4 * person.getDomains() >> []
        1 * person.getCustomAspects() >> []
        2 * person.getLinks() >> []
        output.entity == person
    }

    def "validates scope client"() {
        given: "a scope for another client"
        def scope = Spy(Scope)
        scope.id >> UUID.randomUUID()
        scope.id >> scope.id
        scope.checkSameClient(existingClient) >> { throw new ClientBoundaryViolationException(scope, existingClient) }
        scopeRepository.findByIds([scope.id] as Set) >> [scope]

        when: "creating the new process inside the scope"
        usecase.execute(new CreateElementUseCase.InputData(personState, existingClient, [scope.id] as Set))

        then: "an exception is thrown"
        1 * identifiableFactory.create(Person) >> person
        1 * person.getOwner() >> existingUnit
        3 * person.getDomains() >> []
        1 * person.getCustomAspects() >> []
        2 * person.getLinks() >> []

        thrown(ClientBoundaryViolationException)
    }
}
