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
package org.veo.core.usecase

import org.veo.core.entity.Control
import org.veo.core.entity.Key
import org.veo.core.repository.PagingConfiguration
import org.veo.core.usecase.base.GetEntitiesUseCase.InputData
import org.veo.core.usecase.base.QueryCondition
import org.veo.core.usecase.control.GetControlsUseCase
import org.veo.core.usecase.repository.ControlRepository
import org.veo.core.usecase.repository.EntityLayerSupertypeQuery

class GetControlsUseCaseSpec extends UseCaseSpec {

    ControlRepository controlRepository = Mock()
    EntityLayerSupertypeQuery<Control> query = Mock()
    PagingConfiguration pagingConfiguration = Mock()

    GetControlsUseCase usecase = new GetControlsUseCase(clientRepository, controlRepository, unitHierarchyProvider)

    def setup() {
        controlRepository.query(existingClient) >> query
    }

    def "retrieve all controls for a client"() {
        given:
        def id = Key.newUuid()
        Control control = Mock() {
            getOwner() >> existingUnit
            getId() >> id
        }
        when:
        def output = usecase.execute(new InputData(existingClient, null, null, null,pagingConfiguration))
        then:
        1 * clientRepository.findById(existingClient.id) >> Optional.of(existingClient)
        1 * query.execute(pagingConfiguration) >> singleResult(control, pagingConfiguration)
        output.entities.resultPage*.id == [id]
    }


    def "apply query conditions"() {
        given:
        def id = Key.newUuid()
        Control control = Mock() {
            getOwner() >> existingUnit
            getId() >> id
        }
        when:
        def output = usecase.execute(new InputData(existingClient,
                Mock(QueryCondition) {
                    getValues() >> [existingUnit.id]
                },
                null,
                Mock(QueryCondition) {
                    getValues() >> ["subType 1", "subType 2"]
                },pagingConfiguration))
        then:
        1 * clientRepository.findById(existingClient.id) >> Optional.of(existingClient)
        1 * unitHierarchyProvider.findAllInRoot(existingUnit.id) >> existingUnitHierarchyMembers
        1 * query.whereUnitIn(existingUnitHierarchyMembers)
        1 * query.whereSubTypeIn(["subType 1", "subType 2"].toSet())
        1 * query.execute(pagingConfiguration) >> singleResult(control, pagingConfiguration)
        output.entities.resultPage*.id == [id]
    }
}