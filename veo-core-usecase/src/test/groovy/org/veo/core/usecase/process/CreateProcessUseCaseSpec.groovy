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

import org.veo.core.entity.Process
import org.veo.core.entity.Unit
import org.veo.core.usecase.UseCaseSpec
import org.veo.core.usecase.base.CreateEntityUseCase
import org.veo.core.usecase.repository.ProcessRepository

public class CreateProcessUseCaseSpec extends UseCaseSpec {

    ProcessRepository processRepository = Mock()
    Process process = Mock()
    Process process1 = Mock()
    Unit unit = Mock()


    CreateProcessUseCase usecase = new CreateProcessUseCase(unitRepository,processRepository)
    def "create a process"() {
        process1.owner >> unit
        process1.name >> "John's process"

        given:
        process.getName() >> "John's process"

        when:
        def output = usecase.execute(new CreateEntityUseCase.InputData(process1, existingClient))
        then:
        1 * unitRepository.findById(_) >> Optional.of(existingUnit)
        1 * processRepository.save(process1) >> process
        output.entity != null
        output.entity.name == "John's process"
    }
}
