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
import org.veo.core.repository.ScopeRepository
import org.veo.core.usecase.UseCaseSpec
import org.veo.core.usecase.base.ModifyElementUseCase
import org.veo.core.usecase.common.ETag

class UpdateScopeUseCaseSpec extends UseCaseSpec {

    public static final String USER_NAME = "john"
    ScopeRepository scopeRepository = Mock()

    UpdateScopeUseCase usecase = new UpdateScopeUseCase(scopeRepository)
    def "update a scope"() {
        given:
        def scopeId = Key.newUuid()
        def scope = Mock(Scope)
        scope.getOwner() >> existingUnit
        scope.getId() >> scopeId
        scope.name >> "Updated scope"
        scope.domains >> []
        scope.links >> []

        def existingScope = Mock(Scope) {
            it.id >> scopeId
            it.owner >> existingUnit
            it.domains >> []
        }


        when:
        def eTag = ETag.from(scope.getId().uuidValue(), 0)
        def output = usecase.execute(new ModifyElementUseCase.InputData(scope, existingClient,  eTag, USER_NAME))
        then:

        1 * scopeRepository.findById(scopeId) >> Optional.of(existingScope)
        1 * scope.version(USER_NAME, existingScope)
        1 * scopeRepository.save(_) >> scope
        output.entity != null
        output.entity.name == "Updated scope"
    }
}
