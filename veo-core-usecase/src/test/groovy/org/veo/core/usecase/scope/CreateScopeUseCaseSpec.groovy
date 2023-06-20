/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Jochen Kemnade.
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
package org.veo.core.usecase.scope

import org.veo.core.entity.Key
import org.veo.core.entity.Scope
import org.veo.core.entity.Unit
import org.veo.core.entity.specification.ClientBoundaryViolationException
import org.veo.core.entity.state.ElementState
import org.veo.core.entity.transform.IdentifiableFactory
import org.veo.core.repository.ScopeRepository
import org.veo.core.service.EventPublisher
import org.veo.core.usecase.DesignatorService
import org.veo.core.usecase.UseCaseSpec
import org.veo.core.usecase.base.CreateElementUseCase
import org.veo.core.usecase.decision.Decider
import org.veo.core.usecase.service.EntityStateMapper
import org.veo.core.usecase.service.TypedId

class CreateScopeUseCaseSpec extends UseCaseSpec {

    DesignatorService designatorService = Mock()
    EventPublisher eventPublisher = Mock()
    IdentifiableFactory identifiableFactory = Mock()
    EntityStateMapper entityStateMapper = Mock()
    ScopeRepository scopeRepository = Mock()

    Decider decider = Mock()
    ElementState scopeState = Mock()
    Scope scope = Mock()

    CreateElementUseCase usecase = new CreateElementUseCase(repositoryProvider, designatorService, eventPublisher, identifiableFactory, entityStateMapper, decider)

    def setup() {
        scope.getOwningClient() >> Optional.of(existingClient)
        scopeState.name >> "My scope"
        scopeState.modelInterface >> Scope
        scopeState.owner >> TypedId.from(existingUnit.idAsString, Unit)

        repositoryProvider.getElementRepositoryFor(Scope) >> scopeRepository

        scopeRepository.findByIds([] as Set) >> []
    }

    def "create a scope"() {
        when:
        def output = usecase.execute(new CreateElementUseCase.InputData( scopeState, existingClient, [] as Set))

        then:
        1 * identifiableFactory.create(Scope, _) >> scope
        1 * entityStateMapper.mapState(scopeState, scope, false, _)
        1 * designatorService.assignDesignator(scope, existingClient)
        1 * scopeRepository.save(_) >> { it[0] }
        1 * scope.getOwner() >> existingUnit
        3 * scope.getDomains() >> []
        2 * scope.getCustomAspects() >> []
        2 * scope.getLinks() >> []
        1 * scope.getDomainTemplates() >> []

        when:
        def scope1 = output.entity

        then:
        scope1 == scope
    }

    def "validates super scope client"() {
        given: "a scope for another client"
        def superScope = Mock(Scope)
        superScope.id >> Key.newUuid()
        superScope.checkSameClient(existingClient) >> {
            throw new ClientBoundaryViolationException(superScope, existingClient)
        }
        scopeRepository.findByIds([superScope.id] as Set) >> [superScope]

        when: "creating the new process inside the scope"
        usecase.execute(new CreateElementUseCase.InputData(scopeState, existingClient, [superScope.id] as Set))

        then: "an exception is thrown"
        1 * identifiableFactory.create(Scope, _) >> scope
        1 * scope.getOwner() >> existingUnit
        2 * scope.getDomains() >> []
        2 * scope.getCustomAspects() >> []
        2 * scope.getLinks() >> []
        1 * scope.getDomainTemplates() >> []
        thrown(ClientBoundaryViolationException)
    }
}
