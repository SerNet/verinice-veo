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
import org.veo.core.entity.Person
import org.veo.core.repository.ElementQuery
import org.veo.core.repository.GenericElementRepository
import org.veo.core.repository.PagingConfiguration
import org.veo.core.repository.QueryCondition
import org.veo.core.repository.RepositoryProvider
import org.veo.core.usecase.base.GetElementsUseCase
import org.veo.core.usecase.base.GetElementsUseCase.InputData

class GetElementsUseCaseSpec extends UseCaseSpec {

    GenericElementRepository repo = Mock()
    ElementQuery<Person> query = Mock()
    PagingConfiguration<String> pagingConfiguration = Mock()
    RepositoryProvider repositoryProvider = Mock()

    GetElementsUseCase usecase = new GetElementsUseCase(clientRepository, repo, repositoryProvider, unitRepository)

    def setup() {
        repo.query(existingClient) >> query
    }

    def "retrieve all elements for a client"() {
        given:
        def id = UUID.randomUUID()
        Asset asset = Mock() {
            getOwner() >> existingUnit
            getId() >> id
        }

        when:
        def output = usecase.execute(
                InputData.builder()
                .pagingConfiguration(pagingConfiguration)
                .build(), noRestrictionExistingClient)

        then:
        1 * clientRepository.findById(existingClient.id) >> Optional.of(existingClient)
        1 * query.execute(pagingConfiguration) >> singleResult(asset, pagingConfiguration)
        output.elements.resultPage*.id == [id]
    }

    def "apply query conditions"() {
        given:
        def id = UUID.randomUUID()
        Asset asset = Mock() {
            getOwner() >> existingUnit
            getId() >> id
        }
        def input =
                InputData.builder()
                .unitUuid(Mock(QueryCondition) {
                    getValues() >> [existingUnit.id]
                })
                .subType(Mock(QueryCondition))
                .pagingConfiguration(pagingConfiguration)
                .build()

        when:
        def output = usecase.execute(input, noRestrictionExistingClient)

        then:
        1 * clientRepository.findById(existingClient.id) >> Optional.of(existingClient)
        1 * unitRepository.getById(existingUnit.id) >> existingUnit
        1 * query.whereUnitIn([existingUnit] as Set)
        1 * query.whereSubTypeMatches(input.subType)
        1 * query.execute(pagingConfiguration) >> singleResult(asset, pagingConfiguration)
        output.elements.resultPage*.id == [id]
    }
}