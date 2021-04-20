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
package org.veo.core.usecase.catalogitem;

import org.veo.core.entity.Catalog
import org.veo.core.entity.CatalogItem
import org.veo.core.entity.DomainTemplate
import org.veo.core.entity.Key
import org.veo.core.entity.exception.ModelConsistencyException
import org.veo.core.entity.exception.NotFoundException
import org.veo.core.entity.specification.ClientBoundaryViolationException
import org.veo.core.repository.CatalogItemRepository
import org.veo.core.usecase.UseCaseSpec
import org.veo.core.usecase.catalogitem.GetCatalogItemUseCase
import org.veo.core.usecase.catalogitem.GetCatalogItemUseCase.InputData
import org.veo.core.usecase.repository.ClientRepository

class GetCatalogItemUseCaseSpec extends UseCaseSpec {

    CatalogItemRepository repository = Mock()
    Catalog catalog = Mock()
    CatalogItem catalogitem = Mock()

    DomainTemplate domaintemplate = Mock()
    Key existingDomainId = Key.newUuid()
    Key catalogId = Key.newUuid()
    Key catalogItemId = Key.newUuid()

    GetCatalogItemUseCase usecase = new GetCatalogItemUseCase(repository)

    def setup() {
        existingDomain.getId() >> existingDomainId
        existingDomain.owner >> existingClient

        catalog.getId() >> catalogId
        catalog.getDomainTemplate() >> existingDomain

        catalogitem.getId() >> catalogItemId
        catalogitem.getCatalog() >> catalog

        anotherClient.getDomains() >> []

        repository.findById(catalogItemId) >> Optional.of(catalogitem)
        repository.findById(_) >> Optional.empty()
    }

    def "retrieve a catalogitem"() {
        when:
        existingDomain.isActive() >> true
        def output = usecase.execute(new InputData(catalogItemId,  existingClient))
        then:
        output.catalogItem != null
        output.catalogItem.id == catalogItemId
    }

    def "retrieve a catalogitem for a deleted domain"() {
        when:
        existingDomain.isActive() >> false
        def output = usecase.execute(new InputData(catalogItemId,  existingClient))
        then:
        thrown(NotFoundException)
    }

    def "retrieve a catalogitem for another client"() {
        when:
        existingDomain.isActive() >> true
        def output = usecase.execute(new InputData(catalogItemId,  anotherClient))
        then:
        thrown(ClientBoundaryViolationException)
    }

    def "retrieve an unknown catalogitem"() {
        when:
        existingDomain.isActive() >> true
        def output = usecase.execute(new InputData(Key.newUuid(),  existingClient))
        then:
        thrown(NotFoundException)
    }
}
