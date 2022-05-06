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
import org.veo.core.entity.event.RiskAffectingElementChangeEvent
import org.veo.core.repository.ProcessRepository
import org.veo.core.service.EventPublisher
import org.veo.core.usecase.UseCaseSpec
import org.veo.core.usecase.base.ModifyElementUseCase.InputData
import org.veo.core.usecase.base.ScopeProvider
import org.veo.core.usecase.common.ETag
import org.veo.core.usecase.decision.Decider
import org.veo.core.usecase.risk.RiskValueValidator

public class UpdateProcessUseCaseSpec extends UseCaseSpec {

    ProcessRepository processRepository = Mock()
    EventPublisher eventPublisher = Mock()
    Decider decider = Mock()
    RiskValueValidator riskValueValidator = Mock()

    UpdateProcessUseCase usecase = new UpdateProcessUseCase(processRepository, eventPublisher, decider, riskValueValidator)
    def "update a process"() {
        given:
        def id = Key.newUuid()
        Process process = Mock()
        process.domains >> []
        process.getId() >> id
        process.getName()>> "Updated process"
        process.getOwner() >> existingUnit
        process.version >> 0
        process.modelInterface >> Process
        process.links >> []

        def existingProcess = Mock(Process) {
            it.id >> process.id
            it.name >> "Old process"
            it.owner >> existingUnit
            it.domains >> []
        }

        when:
        def eTag = ETag.from(process.getId().uuidValue(), 0)
        def output = usecase.execute(new InputData(process, existingClient, eTag, "max"))

        then:
        1 * process.version("max", existingProcess)
        1 * process.getOwningClient() >> Optional.of(existingClient)
        1 * processRepository.save(process) >> process
        1 * processRepository.findById(process.id) >> Optional.of(existingProcess)
        1 * eventPublisher.publish({ RiskAffectingElementChangeEvent event->
            event.entityType == Process
            event.entityId == id
        })
        1 * riskValueValidator.validate(process)
        output.entity != null
        output.entity.name == "Updated process"
    }
}
