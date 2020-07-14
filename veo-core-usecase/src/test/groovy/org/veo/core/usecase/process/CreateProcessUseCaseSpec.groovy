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
package org.veo.core.usecase.process

import org.veo.core.entity.Key
import org.veo.core.entity.impl.ProcessImpl
import org.veo.core.entity.transform.TransformTargetToEntityContext
import org.veo.core.usecase.UseCaseSpec
import org.veo.core.usecase.base.CreateEntityInputData
import org.veo.core.usecase.repository.ProcessRepository

public class CreateProcessUseCaseSpec extends UseCaseSpec {

    ProcessRepository processRepository = Mock()

    CreateProcessUseCase usecase = new CreateProcessUseCase(unitRepository,processRepository, transformContextProvider)

    def "create a process"() {
        given:
        TransformTargetToEntityContext targetToEntityContext = Mock()
        when:
        def newProcess = usecase.execute(new CreateEntityInputData(existingUnit.id, new ProcessImpl(Key.newUuid(), "John's process", existingUnit), existingClient))
        then:
        1 * transformContextProvider.createTargetToEntityContext() >> targetToEntityContext
        1 * targetToEntityContext.partialClient() >> targetToEntityContext
        1 * unitRepository.findById(_, _) >> Optional.of(existingUnit)
        1 * processRepository.save({
            it.name == "John's process"
        }) >> { it[0] }
        newProcess != null
        newProcess.name == "John's process"
    }
}
