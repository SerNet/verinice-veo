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

import org.veo.core.entity.Scope
import org.veo.core.repository.DomainRepository
import org.veo.core.repository.ScopeRepository
import org.veo.core.usecase.UseCaseSpec
import org.veo.core.usecase.base.GetElementUseCase

class GetScopeUseCaseSpec extends UseCaseSpec {

    DomainRepository domainRepository = Mock()
    ScopeRepository scopeRepository = Mock()

    GetScopeUseCase usecase = new GetScopeUseCase(domainRepository, scopeRepository)

    def "retrieve a scope"() {
        given:
        def scopeId = UUID.randomUUID()
        def scope = Mock(Scope) {
            getOwner() >> existingUnit
            getId() >> scopeId
        }

        when:
        def output = usecase.execute(new GetElementUseCase.InputData(scopeId, existingClient))

        then:
        1 * scopeRepository.findById(scopeId, false) >> Optional.of(scope)
        output.element != null
        output.element.id == scopeId
    }
}
