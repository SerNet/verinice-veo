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
import org.veo.core.entity.specification.ClientBoundaryViolationException
import org.veo.core.repository.ProcessRepository
import org.veo.core.repository.ScopeRepository
import org.veo.core.service.EventPublisher
import org.veo.core.usecase.DesignatorService
import org.veo.core.usecase.UseCaseSpec
import org.veo.core.usecase.base.CreateElementUseCase
import org.veo.core.usecase.decision.Decider

class CreateProcessUseCaseSpec extends UseCaseSpec {

    ScopeRepository scopeRepository = Mock()
    ProcessRepository processRepository = Mock()
    Process process = Mock()
    Unit unit = Mock()
    DesignatorService designatorService = Mock()
    EventPublisher eventPublisher = Mock()
    Decider decider = Mock()

    CreateProcessUseCase usecase = new CreateProcessUseCase(unitRepository,scopeRepository, processRepository, designatorService, eventPublisher, decider)

    def setup() {
        def id = Key.newUuid()
        process.domains >> []
        process.domainTemplates >> []
        process.owner >> unit
        process.name >> "John's process"
        process.modelInterface >> Process
        process.id >> id
        process.customAspects >> []
        process.links >> []
        process.owningClient >> Optional.of(existingClient)

        unit.domains >> []

        unitRepository.findById(_) >> Optional.of(existingUnit)
        scopeRepository.findByIds([] as Set) >> []
    }

    def "create a process"() {
        when:
        def output = usecase.execute(new CreateElementUseCase.InputData(process, existingClient, [] as Set))

        then:
        1 * processRepository.save(process) >> process
        1 * designatorService.assignDesignator(process, existingClient)
        1 * eventPublisher.publish({ RiskAffectingElementChangeEvent event->
            event.entityType == Process
            event.entityId == process.id
        })
        output.entity != null
        output.entity.name == "John's process"
    }

    def "validates scope client"() {
        given: "a scope for another client"
        def scope = Mock(Scope)
        scope.id >> Key.newUuid()
        scope.checkSameClient(existingClient) >> { throw new ClientBoundaryViolationException(scope, existingClient) }
        scopeRepository.findByIds([scope.id] as Set) >> [scope]

        when: "creating the new process inside the scope"
        usecase.execute(new CreateElementUseCase.InputData(process, existingClient, [scope.id] as Set))

        then: "an exception is thrown"
        thrown(ClientBoundaryViolationException)
    }
}
