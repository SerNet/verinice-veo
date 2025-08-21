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
package org.veo.core.usecase.process

import org.veo.core.UserAccessRights
import org.veo.core.entity.Process
import org.veo.core.repository.DomainRepository
import org.veo.core.repository.ProcessRepository
import org.veo.core.usecase.UseCaseSpec
import org.veo.core.usecase.base.GetElementUseCase

class GetProcessUseCaseSpec extends UseCaseSpec {

    ProcessRepository processRepository = Mock()
    DomainRepository domainRepository = Mock()
    UserAccessRights user = Mock()

    GetProcessUseCase usecase = new GetProcessUseCase(processRepository, domainRepository)

    def "retrieve a process"() {
        given:
        def id = UUID.randomUUID()
        Process process = Mock()
        process.getOwner() >> existingUnit
        process.getId() >> id

        when:
        def output = usecase.execute(new GetElementUseCase.InputData(id), user)

        then:
        1 * processRepository.findById(id, _, user) >> Optional.of(process)
        output.element != null
        output.element.id == id
    }
}