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
package org.veo.core.usecase.process

import org.veo.core.entity.Key
import org.veo.core.entity.Process
import org.veo.core.entity.Scope
import org.veo.core.entity.Unit
import org.veo.core.entity.event.RiskAffectingElementChangeEvent
import org.veo.core.entity.ref.TypedId
import org.veo.core.entity.specification.ClientBoundaryViolationException
import org.veo.core.entity.state.ProcessState
import org.veo.core.repository.ProcessRepository
import org.veo.core.repository.ScopeRepository
import org.veo.core.service.EventPublisher
import org.veo.core.usecase.DesignatorService
import org.veo.core.usecase.UseCaseSpec
import org.veo.core.usecase.base.CreateElementUseCase
import org.veo.core.usecase.decision.Decider
import org.veo.core.usecase.service.EntityStateMapper

class CreateProcessUseCaseSpec extends UseCaseSpec {

    DesignatorService designatorService = Mock()
    EntityStateMapper entityStateMapper = Mock()
    EventPublisher eventPublisher = Mock()

    Decider decider = Mock()
    ScopeRepository scopeRepository = Mock()
    ProcessRepository processRepository = Mock()

    ProcessState processState = Mock()
    Process process = Mock{
        controlImplementations >> []
        requirementImplementations >> []
        links >> []
        parts >> []
        composites >> []
        scopes >> []
        appliedCatalogItems >> []
    }

    CreateElementUseCase usecase = new CreateElementUseCase(refResolverFactory, repositoryProvider, designatorService, eventPublisher, identifiableFactory, entityStateMapper, decider)

    def setup() {
        processState.name >> "John's process"
        processState.modelInterface >> Process
        processState.owner >> TypedId.from(existingUnit.idAsUUID, Unit)

        repositoryProvider.getElementRepositoryFor(Scope) >> scopeRepository
        repositoryProvider.getElementRepositoryFor(Process) >> processRepository
        scopeRepository.findByIds([] as Set) >> []
    }

    def "create a process"() {
        when:
        def output = usecase.execute(new CreateElementUseCase.InputData(processState, existingClient, [] as Set))

        then:
        1 * identifiableFactory.create(Process) >> process
        1 * entityStateMapper.mapState(processState, process, false, _)
        1 * process.getOwner() >> existingUnit
        4 * process.getDomains() >> []
        1 * process.getCustomAspects() >> []
        2 * process.getLinks() >> []
        1 * process.owningClient >> Optional.of(existingClient)
        1 * process.modelInterface >> Process
        1 * processRepository.save(process) >> process
        1 * designatorService.assignDesignator(process, existingClient)
        1 * eventPublisher.publish({ RiskAffectingElementChangeEvent event->
            event.entityType == Process
            event.entityId == process.id
        })
        output.entity == process
    }

    def "validates scope client"() {
        given: "a scope for another client"
        def scope = Spy(Scope)
        scope.id >> Key.newUuid()
        scope.idAsUUID >> scope.id.value()
        scope.checkSameClient(existingClient) >> { throw new ClientBoundaryViolationException(scope, existingClient) }
        scopeRepository.findByIds([scope.id] as Set) >> [scope]

        when: "creating the new process inside the scope"
        usecase.execute(new CreateElementUseCase.InputData(processState, existingClient, [scope.id] as Set))

        then: "an exception is thrown"
        1 * identifiableFactory.create(Process) >> process
        1 * process.getOwner() >> existingUnit
        3 * process.getDomains() >> []
        1 * process.getCustomAspects() >> []
        2 * process.getLinks() >> []

        thrown(ClientBoundaryViolationException)
    }
}
