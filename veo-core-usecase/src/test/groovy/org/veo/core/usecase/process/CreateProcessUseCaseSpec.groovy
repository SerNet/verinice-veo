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
import org.veo.core.entity.Unit
import org.veo.core.entity.event.RiskComponentChangeEvent
import org.veo.core.repository.ProcessRepository
import org.veo.core.service.EventPublisher
import org.veo.core.usecase.DesignatorService
import org.veo.core.usecase.UseCaseSpec
import org.veo.core.usecase.base.CreateElementUseCase
import org.veo.core.usecase.decision.Decider

class CreateProcessUseCaseSpec extends UseCaseSpec {

    ProcessRepository processRepository = Mock()
    Process process = Mock()
    Unit unit = Mock()
    DesignatorService designatorService = Mock()
    EventPublisher eventPublisher = Mock()
    Decider decider = Mock()

    CreateProcessUseCase usecase = new CreateProcessUseCase(unitRepository, processRepository, designatorService, eventPublisher, decider)
    def "create a process"() {
        def id = Key.newUuid()
        process.domains >> []
        process.owner >> unit
        process.name >> "John's process"
        process.modelInterface >> Process
        process.id >> id
        process.links >> []

        when:
        def output = usecase.execute(new CreateElementUseCase.InputData(process, existingClient))
        then:
        1 * unitRepository.findById(_) >> Optional.of(existingUnit)
        1 * processRepository.save(process) >> process
        1 * designatorService.assignDesignator(process, existingClient)
        1 * eventPublisher.publish({RiskComponentChangeEvent event->
            event.entityType == Process
            event.entityId == id
        })
        output.entity != null
        output.entity.name == "John's process"
    }
}
