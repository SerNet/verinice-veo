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
import org.veo.core.repository.ScopeRepository
import org.veo.core.usecase.DesignatorService
import org.veo.core.usecase.UseCaseSpec
import org.veo.core.usecase.base.CreateElementUseCase
import org.veo.core.usecase.decision.Decider

class CreateScopeUseCaseSpec extends UseCaseSpec {

    ScopeRepository scopeRepository = Mock()
    DesignatorService designatorService = Mock()
    Scope scope = Mock()
    Decider decider = Mock()

    CreateScopeUseCase usecase = new CreateScopeUseCase(unitRepository, scopeRepository, designatorService, decider)
    Unit unit = Mock()

    def setup() {
        scope.name >> "My scope"
        scope.owner >> unit
        scope.customAspects >> []
        scope.links >> []
        scope.domains >> []
        scope.domainTemplates >> []

        unitRepository.findById(_) >> Optional.of(existingUnit)
        scopeRepository.getByIds([] as Set) >> []
    }

    def "create a scope"() {
        when:
        def output = usecase.execute(new CreateElementUseCase.InputData( scope, existingClient, [] as Set))

        then:
        1 * scopeRepository.save(_) >> { it[0] }
        1 * designatorService.assignDesignator(scope, existingClient)
        when:
        def scope1 = output.entity

        then:
        scope1 != null
        scope1.name == "My scope"
    }

    def "validates super scope client"() {
        given: "a scope for another client"
        def superScope = Mock(Scope)
        superScope.id >> Key.newUuid()
        superScope.checkSameClient(existingClient) >> {
            throw new ClientBoundaryViolationException(superScope, existingClient)
        }
        scopeRepository.getByIds([superScope.id] as Set) >> [superScope]

        when: "creating the new process inside the scope"
        usecase.execute(new CreateElementUseCase.InputData(scope, existingClient, [superScope.id] as Set))

        then: "an exception is thrown"
        thrown(ClientBoundaryViolationException)
    }
}
