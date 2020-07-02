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
package org.veo.core.usecase.base

import org.veo.core.entity.Key
import org.veo.core.entity.Person
import org.veo.core.entity.Process
import org.veo.core.usecase.UseCaseSpec
import org.veo.core.usecase.base.DeleteEntityUseCase.InputData
import org.veo.core.usecase.repository.PersonRepository
import org.veo.core.usecase.repository.ProcessRepository

public class DeleteEntityUseCaseSpec extends UseCaseSpec {

    ProcessRepository processRepository = Mock()
    PersonRepository personRepository = Mock()

    def usecase = new DeleteEntityUseCase(repositoryProvider)

    def "Delete a process" () {
        def id = Key.newUuid()
        Process process = Mock() {
            getOwner() >> existingUnit
            getId() >> id
        }
        when:
        usecase.execute(new InputData(Process,id, existingClient))
        then:
        1 * repositoryProvider.getEntityLayerSupertypeRepositoryFor(Process) >> processRepository
        1 * processRepository.findById(id) >> Optional.of(process)
        1 * processRepository.deleteById(id)
    }

    def "Delete a person" () {
        def id = Key.newUuid()
        Person person = Mock() {
            getOwner() >> existingUnit
            getId() >> id
        }
        when:
        usecase.execute(new InputData(Person,id, existingClient))
        then:
        1 * repositoryProvider.getEntityLayerSupertypeRepositoryFor(Person) >> personRepository
        1 * personRepository.findById(id) >> Optional.of(person)
        1 * personRepository.deleteById(id)
    }
}
