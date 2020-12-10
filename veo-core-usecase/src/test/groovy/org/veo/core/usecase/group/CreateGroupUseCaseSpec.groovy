/*******************************************************************************
 * Copyright (c) 2020 Jochen Kemnade.
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
package org.veo.core.usecase.group

import org.veo.core.entity.GroupType
import org.veo.core.entity.ModelGroup
import org.veo.core.usecase.UseCaseSpec
import org.veo.core.usecase.group.CreateGroupUseCase.InputData

import spock.lang.Unroll

class CreateGroupUseCaseSpec extends UseCaseSpec {

    public static final String USER_NAME = "john"
    CreateGroupUseCase usecase = new CreateGroupUseCase(unitRepository,repositoryProvider, entityFactory)

    @Unroll
    def "create a #type group"() {
        given:
        def repository = Mock(Class.forName("org.veo.core.usecase.repository.${type}Repository"))

        ModelGroup group = Mock(Class.forName("org.veo.core.entity.groups.${type}Group"))
        group.name >> "$type group 1"

        entityFactory.createGroup(type, _, "$type group 1", existingUnit) >> group

        when:
        def output = usecase.execute(new InputData(existingUnit.id, "$type group 1", type, existingClient, USER_NAME))

        then:
        1 * unitRepository.findById(_) >> Optional.of(existingUnit)
        1 * repositoryProvider.getEntityLayerSupertypeRepositoryFor(type.entityClass) >> repository
        1 * group.version(USER_NAME, null)
        1 * repository.save(_)  >> { it[0] }
        when:
        def group1 = output.group


        then:
        group1 != null
        group1.name == "$type group 1"

        where:
        type << GroupType.values()
    }
}
