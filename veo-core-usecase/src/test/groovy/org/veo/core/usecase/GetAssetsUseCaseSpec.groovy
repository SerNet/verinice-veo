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
package org.veo.core.usecase

import org.veo.core.entity.Asset
import org.veo.core.entity.Key
import org.veo.core.entity.Person
import org.veo.core.repository.AssetRepository
import org.veo.core.repository.ElementQuery
import org.veo.core.repository.PagingConfiguration
import org.veo.core.repository.QueryCondition
import org.veo.core.usecase.asset.GetAssetsUseCase
import org.veo.core.usecase.base.GetElementsUseCase
import org.veo.core.usecase.base.GetElementsUseCase.InputData

class GetAssetsUseCaseSpec extends UseCaseSpec {

    AssetRepository assetRepository = Mock()
    ElementQuery<Person> query = Mock()
    PagingConfiguration pagingConfiguration = Mock()

    GetAssetsUseCase usecase = new GetAssetsUseCase(clientRepository, assetRepository, unitHierarchyProvider)

    def setup() {
        assetRepository.query(existingClient) >> query
    }

    def "retrieve all assets for a client"() {
        given:
        def id = Key.newUuid()
        Asset asset = Mock() {
            getOwner() >> existingUnit
            getId() >> id
        }

        when:
        def output = usecase.execute(new GetElementsUseCase.RiskAffectedInputData(existingClient, null, null, null, null, null, null, null, null, null, null, null, null, null, null, pagingConfiguration, false))

        then:
        1 * clientRepository.findById(existingClient.id) >> Optional.of(existingClient)
        1 * query.execute(pagingConfiguration) >> singleResult(asset, pagingConfiguration)
        output.elements.resultPage*.id == [id]
    }

    def "apply query conditions"() {
        given:
        def id = Key.newUuid()
        Asset asset = Mock() {
            getOwner() >> existingUnit
            getId() >> id
        }
        def input = new GetElementsUseCase.RiskAffectedInputData(existingClient, Mock(QueryCondition) {
            getValues() >> [existingUnit.id]
        }, null, null, Mock(QueryCondition), null, null, null, null, null, null, null, null, null, null, pagingConfiguration, false)

        when:
        def output = usecase.execute(input)

        then:
        1 * clientRepository.findById(existingClient.id) >> Optional.of(existingClient)
        1 * unitHierarchyProvider.findAllInRoot(existingUnit.id) >> existingUnitHierarchyMembers
        1 * query.whereUnitIn(existingUnitHierarchyMembers)
        1 * query.whereSubTypeMatches(input.subType)
        1 * query.execute(pagingConfiguration) >> singleResult(asset, pagingConfiguration)
        output.elements.resultPage*.id == [id]
    }
}