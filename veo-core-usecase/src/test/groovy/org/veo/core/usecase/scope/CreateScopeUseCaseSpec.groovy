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

import org.veo.core.entity.Scope
import org.veo.core.entity.Unit
import org.veo.core.usecase.UseCaseSpec
import org.veo.core.usecase.base.CreateEntityUseCase
import org.veo.core.usecase.repository.ScopeRepository

import spock.lang.Unroll

class CreateScopeUseCaseSpec extends UseCaseSpec {

    public static final String USER_NAME = "john"
    ScopeRepository entityScopeRepository = Mock()

    CreateScopeUseCase usecase = new CreateScopeUseCase(unitRepository,entityScopeRepository)
    Unit unit = Mock()

    @Unroll
    def "create a scope"() {
        given:

        Scope  scope = Mock()
        scope.name >> "My scope"
        scope.owner >> unit

        when:
        def output = usecase.execute(new CreateEntityUseCase.InputData( scope, existingClient, USER_NAME))

        then:
        1 * unitRepository.findById(_) >> Optional.of(existingUnit)
        1 *  scope.version(USER_NAME, null)
        1 * entityScopeRepository.save(_) >> { it[0] }
        when:
        def scope1 = output.entity


        then:
        scope1 != null
        scope1.name == "My scope"
    }
}
