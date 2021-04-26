/*******************************************************************************
 * Copyright (c) 2021 Jochen Kemnade.
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.veo.core.usecase.scope

import org.veo.core.entity.Key
import org.veo.core.entity.Scope
import org.veo.core.usecase.UseCase
import org.veo.core.usecase.UseCaseSpec
import org.veo.core.usecase.repository.ScopeRepository

import spock.lang.Unroll

class GetScopeUseCaseSpec extends UseCaseSpec {

    ScopeRepository scopeRepository = Mock()

    GetScopeUseCase usecase = new GetScopeUseCase(scopeRepository)
    @Unroll
    def "retrieve a scope"() {
        given:
        def repository = Mock(ScopeRepository)
        def scopeId = Key.newUuid()
        def scope = Mock(Scope) {
            getOwner() >> existingUnit
            getId() >> scopeId
        }
        when:
        def output = usecase.execute(new UseCase.IdAndClient(scopeId, existingClient))
        then:
        1 * scopeRepository.findById(scopeId) >> Optional.of(scope)
        output.scope != null
        output.scope.id == scopeId
    }
}
