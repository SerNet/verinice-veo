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

import org.veo.core.entity.Key
import org.veo.core.entity.Process
import org.veo.core.repository.PagingConfiguration
import org.veo.core.repository.ProcessQuery
import org.veo.core.repository.ProcessRepository
import org.veo.core.repository.QueryCondition
import org.veo.core.usecase.UseCaseSpec
import org.veo.core.usecase.process.GetProcessesUseCase.InputData

class GetProcessesUseCaseSpec extends UseCaseSpec {

    ProcessRepository processRepository = Mock()
    ProcessQuery query = Mock()
    PagingConfiguration pagingConfiguration = Mock()

    GetProcessesUseCase usecase = new GetProcessesUseCase(clientRepository, processRepository, unitHierarchyProvider)

    def setup() {
        processRepository.query(existingClient) >> query
    }

    def "retrieve all processes for a client"() {
        given:
        def id = Key.newUuid()
        Process process = Mock()
        process.getOwner() >> existingUnit
        process.getId() >> id
        when:
        def output = usecase.execute(new InputData(existingClient, null, null, null, null, pagingConfiguration))
        then:
        1 * clientRepository.findById(existingClient.id) >> Optional.of(existingClient)
        1 * query.execute(pagingConfiguration) >> singleResult(process, pagingConfiguration)

        output.entities.resultPage*.id == [id]
    }


    def "apply query conditions"() {
        given:
        def id = Key.newUuid()
        Process process = Mock() {
            getOwner() >> existingUnit
            getId() >> id
        }
        def input = new InputData(existingClient, Mock(QueryCondition) {
            getValues() >> [existingUnit.id]
        }, null, Mock(QueryCondition), Mock(QueryCondition), pagingConfiguration)
        when:
        def output = usecase.execute(input)
        then:
        1 * clientRepository.findById(existingClient.id) >> Optional.of(existingClient)
        1 * unitHierarchyProvider.findAllInRoot(existingUnit.id) >> existingUnitHierarchyMembers
        1 * query.whereUnitIn(existingUnitHierarchyMembers)
        1 * query.whereSubTypeMatches(input.subType)
        1 * query.whereStatusMatches(input.status)
        1 * query.execute(pagingConfiguration) >> singleResult(process, pagingConfiguration)
        output.entities.resultPage*.id == [id]
    }
}