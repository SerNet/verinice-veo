/*******************************************************************************
 * Copyright (c) 2019 Urs Zeidler.
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
package org.veo.core.usecase.catalog;

import org.veo.core.entity.Catalog
import org.veo.core.entity.Domain
import org.veo.core.entity.DomainTemplate
import org.veo.core.entity.Key
import org.veo.core.entity.exception.ModelConsistencyException
import org.veo.core.entity.exception.NotFoundException
import org.veo.core.repository.CatalogRepository
import org.veo.core.usecase.UseCaseSpec
import org.veo.core.usecase.catalog.GetCatalogsUseCase
import org.veo.core.usecase.catalog.GetCatalogsUseCase.InputData
import org.veo.core.usecase.domain.GetDomainUseCaseSpec
import org.veo.core.usecase.repository.ClientRepository

class GetCatalogsUseCaseSpec extends UseCaseSpec {

    Catalog catalog = Mock()
    DomainTemplate domaintemplate = Mock()
    Key existingDomainId = Key.newUuid()
    Key catalogId = Key.newUuid()
    Key catalog1Id = Key.newUuid()

    GetCatalogsUseCase usecase = new GetCatalogsUseCase()

    def setup() {
        existingDomain.getId() >> existingDomainId
        existingDomain.owner >> existingClient
        existingDomain.active >> true

        catalog.getId() >> catalogId
        catalog.getDomainTemplate() >> existingDomain
        existingDomain.catalogs >> [catalog]

        anotherClient.getDomains() >> []

        Catalog catalog1 = Mock()
        catalog1.getId() >> catalog1Id
        catalog1.getDomainTemplate() >> domaintemplate
    }


    def "retrieve all catalogs for client"() {
        when:
        def output = usecase.execute(new InputData(Optional.empty(),  existingClient))
        then:
        output.catalogs.size() == 1
    }

    def "retrieve all catalogs for a domain"() {
        when:
        def output = usecase.execute(new InputData(Optional.of(existingDomainId),  existingClient))
        then:
        output.catalogs.size() == 1
    }

    def "retrieve a catalog for a domain of another client"() {
        when:
        def output = usecase.execute(new InputData(Optional.of(existingDomainId),  anotherClient))
        then:
        output.catalogs.size() == 0
    }

    def "retrieve a catalog for an unknown domain"() {
        when:
        def output = usecase.execute(new InputData(Optional.of(Key.newUuid()),  existingClient))
        then:
        output.catalogs.size() == 0
    }
}
