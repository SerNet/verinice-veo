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
package org.veo.core.usecase.base

import org.veo.core.entity.Asset
import org.veo.core.entity.Control
import org.veo.core.entity.Document
import org.veo.core.entity.Person
import org.veo.core.entity.Process
import org.veo.core.entity.Scope
import org.veo.core.entity.event.RiskAffectingElementChangeEvent
import org.veo.core.repository.AssetRepository
import org.veo.core.repository.ControlRepository
import org.veo.core.repository.DocumentRepository
import org.veo.core.repository.PersonRepository
import org.veo.core.repository.ProcessRepository
import org.veo.core.repository.ScopeRepository
import org.veo.core.service.EventPublisher
import org.veo.core.usecase.UseCaseSpec
import org.veo.core.usecase.base.DeleteElementUseCase.InputData

class DeleteElementUseCaseSpec extends UseCaseSpec {

    AssetRepository assetRepository = Mock()
    ControlRepository controlRepository = Mock()
    DocumentRepository documentRepository = Mock()
    PersonRepository personRepository = Mock()
    ProcessRepository processRepository = Mock()
    ScopeRepository scopeRepository = Mock()
    EventPublisher eventPublisher = Mock()

    def usecase = new DeleteElementUseCase(repositoryProvider, eventPublisher)

    def setup() {
        repositoryProvider.getElementRepositoryFor(Asset) >> assetRepository
        repositoryProvider.getElementRepositoryFor(Control) >> controlRepository
        repositoryProvider.getElementRepositoryFor(Document) >> documentRepository
        repositoryProvider.getElementRepositoryFor(Person) >> personRepository
        repositoryProvider.getElementRepositoryFor(Process) >> processRepository
        repositoryProvider.getElementRepositoryFor(Scope) >> scopeRepository
    }

    def "Delete a process" () {
        def id = UUID.randomUUID()
        Process process = Mock() {
            getOwner() >> existingUnit
            getId() >> id
            getModelInterface() >> Process
            getOwningClient() >> Optional.of(existingClient)
            getRequirementImplementations() >> []
            getControlImplementations() >> []
        }

        when:
        usecase.execute(new InputData(Process,id, existingClient))

        then:
        1 * processRepository.findById(id) >> Optional.of(process)
        1 * processRepository.deleteById(id)
        1 * eventPublisher.publish({ RiskAffectingElementChangeEvent event->
            event.entityType == Process
            event.entityId == id
        })
    }

    def "Delete a person" () {
        def id = UUID.randomUUID()
        Person person = Mock() {
            getOwner() >> existingUnit
            getId() >> id
        }

        when:
        usecase.execute(new InputData(Person,id, existingClient))

        then:
        1 * personRepository.findById(id) >> Optional.of(person)
        1 * personRepository.deleteById(id)
    }

    def "Delete a scope"() {
        given:
        def scopeId = UUID.randomUUID()
        Scope scope = Mock() {
            getOwner() >> existingUnit
            getId() >> scopeId
            getOwningClient() >> Optional.of(existingClient)
            getModelInterface() >> Scope
            getRequirementImplementations() >> []
            getControlImplementations() >> []
        }

        when:
        usecase.execute(new InputData(Scope, scopeId, existingClient))

        then:
        1 * scopeRepository.findById(scopeId) >> Optional.of(scope)
        1 * scopeRepository.deleteById(scopeId)
        1 * eventPublisher.publish({ RiskAffectingElementChangeEvent event->
            event.entityType == Scope
            event.entityId == scopeId
        })
    }
}
