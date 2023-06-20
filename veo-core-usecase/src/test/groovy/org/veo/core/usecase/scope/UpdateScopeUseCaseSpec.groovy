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
import org.veo.core.entity.event.RiskAffectingElementChangeEvent
import org.veo.core.entity.state.ScopeState
import org.veo.core.repository.ScopeRepository
import org.veo.core.service.EventPublisher
import org.veo.core.usecase.UseCaseSpec
import org.veo.core.usecase.base.ModifyElementUseCase
import org.veo.core.usecase.common.ETag
import org.veo.core.usecase.decision.Decider
import org.veo.core.usecase.service.EntityStateMapper

class UpdateScopeUseCaseSpec extends UseCaseSpec {

    public static final String USER_NAME = "john"
    ScopeRepository scopeRepository = Mock()
    Decider decider = Mock()
    EntityStateMapper entityStateMapper = new EntityStateMapper()
    EventPublisher eventPublisher = Mock()

    UpdateScopeUseCase usecase = new UpdateScopeUseCase(repositoryProvider, decider, entityStateMapper, eventPublisher)
    def "update a scope"() {
        given:
        def scopeId = Key.newUuid()
        def scope = Mock(ScopeState)
        scope.getId() >> scopeId.uuidValue()
        scope.name >> "Updated scope"
        scope.domainAssociationStates >> []
        scope.members >> []

        def existingScope = Mock(Scope) {
            it.id >> scopeId
            it.owner >> existingUnit
            it.domains >> []
            it.customAspects >> []
            it.links >> []
            it.domainTemplates >> []
            it.modelInterface >> Scope
        }

        when:
        def eTag = ETag.from(scopeId.uuidValue(), 0)
        def output = usecase.execute(new ModifyElementUseCase.InputData(scopeId.uuidValue(), scope, existingClient,  eTag, USER_NAME))

        then:
        1 * repositoryProvider.getElementRepositoryFor(Scope) >> scopeRepository
        1 * existingScope.getOwningClient() >> Optional.of(existingClient)
        1 * scopeRepository.findById(scopeId) >> Optional.of(existingScope)
        1 * scopeRepository.getById(scopeId, existingClient.id) >> existingScope
        1 * scopeRepository.save(existingScope) >> existingScope
        1 * eventPublisher.publish({ RiskAffectingElementChangeEvent event->
            event.entityType == Scope
            event.entityId == scopeId
        })
        1 * existingScope.setName("Updated scope")
        output.entity != null
    }
}
