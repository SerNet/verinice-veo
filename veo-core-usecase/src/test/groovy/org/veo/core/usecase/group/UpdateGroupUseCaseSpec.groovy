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

import java.util.function.Function

import org.veo.core.entity.GroupType
import org.veo.core.entity.Key
import org.veo.core.entity.ModelGroup
import org.veo.core.entity.transform.TransformTargetToEntityContext
import org.veo.core.usecase.UseCaseSpec
import org.veo.core.usecase.common.ETag
import org.veo.core.usecase.group.UpdateGroupUseCase.InputData
import org.veo.core.usecase.repository.EntityGroupRepository

import spock.lang.Unroll

class UpdateGroupUseCaseSpec extends UseCaseSpec {

    public static final String USER_NAME = "john"
    EntityGroupRepository entityGroupRepository = Mock()

    PutGroupUseCase usecase = new PutGroupUseCase(entityGroupRepository)
    @Unroll
    def "update a #type group"() {
        given:
        TransformTargetToEntityContext targetToEntityContext = Mock()
        def repository = Mock(EntityGroupRepository)
        def groupId = Key.newUuid()
        def group = Mock(ModelGroup)
        group.getOwner() >> existingUnit
        group.getId() >> groupId
        group.name >> "Updated $type group"

        Function<Class<ModelGroup>, ModelGroup<?>> groupMapper = Mock()
        def existingGroup = Mock(ModelGroup) {
            it.id >> groupId
        }


        when:
        def eTag = ETag.from(group.getId().uuidValue(), 0)
        def output = usecase.execute(new InputData(groupMapper, existingClient, groupId.uuidValue(), eTag, USER_NAME))
        then:

        //        1 * targetToEntityContext.partialDomain() >> targetToEntityContext
        1 * entityGroupRepository.findById(groupId) >> Optional.of(existingGroup)
        1 * groupMapper.apply(_) >> group
        1 * group.version(USER_NAME, existingGroup)
        1 * entityGroupRepository.save(_) >> group
        output.group != null
        output.group.name == "Updated $type group"
        //        output.group.getClass() == type.groupClass // check removed as it is a mock
        where:
        type << GroupType.values()
    }
}
