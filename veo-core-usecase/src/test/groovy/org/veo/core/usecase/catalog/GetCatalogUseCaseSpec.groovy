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
import org.veo.core.entity.DomainTemplate
import org.veo.core.entity.Key
import org.veo.core.entity.exception.NotFoundException
import org.veo.core.entity.specification.ClientBoundaryViolationException
import org.veo.core.repository.CatalogRepository
import org.veo.core.usecase.UseCaseSpec
import org.veo.core.usecase.catalog.GetCatalogUseCase.InputData

class GetCatalogUseCaseSpec extends UseCaseSpec {

    CatalogRepository repository = Mock()
    Catalog catalog = Mock()
    DomainTemplate domaintemplate = Mock()
    Key existingDomainId = Key.newUuid()
    Key catalogId = Key.newUuid()
    Key catalog1Id = Key.newUuid()

    GetCatalogUseCase usecase = new GetCatalogUseCase(repository)

    def setup() {
        existingDomain.getId() >> existingDomainId
        existingDomain.owner >> existingClient

        catalog.getId() >> catalogId
        catalog.getDomainTemplate() >> existingDomain

        anotherClient.getDomains() >> []

        Catalog catalog1 = Mock()
        catalog1.getId() >> catalog1Id
        catalog1.getDomainTemplate() >> domaintemplate

        repository.findById(catalogId) >> Optional.of(catalog)
        repository.findById(catalog1Id) >> Optional.of(catalog1)
        repository.findById(_) >> Optional.empty()
    }


    def "retrieve a catalog"() {
        when:
        existingDomain.isActive() >> true
        def output = usecase.execute(new InputData(catalogId,  existingClient))
        then:
        output.catalog != null
        output.catalog.id == catalogId
    }

    def "retrieve a catalog for another client"() {
        when:
        existingDomain.isActive() >> true
        def output = usecase.execute(new InputData(catalogId,  anotherClient))
        then:
        thrown(ClientBoundaryViolationException)
    }

    def "retrieve a catalog for inactive domain"() {
        when:
        existingDomain.isActive() >> false
        def output = usecase.execute(new InputData(catalogId,  existingClient))
        then:
        thrown(NotFoundException)
    }

    def "retrieve an unknown catalog"() {
        when:
        existingDomain.isActive() >> true
        def output = usecase.execute(new InputData(Key.newUuid(),  existingClient))
        then:
        thrown(NotFoundException)
    }

    def "retrieve a catalog for domain template"() {
        anotherClient.getDomains() >> [domaintemplate]
        when:
        def output = usecase.execute(new InputData(catalog1Id,  anotherClient))
        then:
        thrown(IllegalArgumentException)
    }
}
