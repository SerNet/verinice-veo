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
import org.veo.core.entity.state.ProcessState
import org.veo.core.repository.ProcessRepository
import org.veo.core.service.EventPublisher
import org.veo.core.usecase.UseCaseSpec
import org.veo.core.usecase.base.ModifyElementUseCase.InputData
import org.veo.core.usecase.common.ETag
import org.veo.core.usecase.decision.Decider
import org.veo.core.usecase.service.EntityStateMapper
import org.veo.core.usecase.service.RefResolverFactory

public class UpdateProcessUseCaseSpec extends UseCaseSpec {

    ProcessRepository processRepository = Mock()
    EventPublisher eventPublisher = Mock()
    Decider decider = Mock()
    EntityStateMapper entityStateMapper = new EntityStateMapper()

    UpdateProcessUseCase usecase = new UpdateProcessUseCase(repositoryProvider, eventPublisher, decider, entityStateMapper, refResolverFactory)
    def "update a process"() {
        given:
        def id = Key.newUuid()
        ProcessState process = Mock()
        process.domainAssociationStates >> []
        process.controlImplementationStates >> []
        process.getId() >> id.uuidValue()
        process.getName()>> "Updated process"
        process.parts >> []

        def existingProcess = Mock(Process) {
            it.id >> id
            it.name >> "Old process"
            it.owner >> existingUnit
            it.domains >> []
            it.customAspects >> []
            it.controlImplementations >> []
            it.requirementImplementations >> []
            it.links >> []
            it.parts >> []
            it.composites >> []
            it.scopes >> []
            it.domainTemplates >> []
            it.appliedCatalogItems >> []
            it.modelInterface >> Process
            it.version >> 0
        }

        when:
        def eTag = ETag.from(id.uuidValue(), 0)
        def output = usecase.execute(new InputData(id.uuidValue(), process, existingClient, eTag, "max"))

        then:
        1 * repositoryProvider.getElementRepositoryFor(Process) >> processRepository
        _ * existingProcess.getOwningClient() >> Optional.of(existingClient)
        1 * processRepository.save(existingProcess) >> existingProcess
        1 * processRepository.findById(id) >> Optional.of(existingProcess)
        1 * processRepository.getById(id, existingClient.id) >> existingProcess
        1 * eventPublisher.publish({ RiskAffectingElementChangeEvent event->
            event.entityType == Process
            event.entityId == id
        })
        1 * existingProcess.setName("Updated process")
        output.entity != null
    }
}
