/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2019  Urs Zeidler.
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
package org.veo.core.usecase.catalogitem

import org.veo.core.entity.CatalogItem
import org.veo.core.entity.Client
import org.veo.core.entity.exception.NotFoundException
import org.veo.core.repository.CatalogItemRepository
import org.veo.core.repository.DomainRepository
import org.veo.core.usecase.UseCaseSpec
import org.veo.rest.security.NoRestrictionAccessRight

class GetCatalogItemUseCaseSpec extends UseCaseSpec {
    CatalogItem catalogItem = Mock()
    Client existingClient = Mock {
        id >> UUID.randomUUID()
    }
    def existingDomainId = UUID.randomUUID()
    def catalogItemId = UUID.randomUUID()

    DomainRepository domainRepository = Mock()
    CatalogItemRepository catalogItemRepository = Mock()
    GetCatalogItemUseCase usecase = new GetCatalogItemUseCase(domainRepository, catalogItemRepository)

    def setup() {
        domainRepository.getActiveById(existingDomainId, existingClient.id) >> existingDomain
        domainRepository.getActiveById(_, _) >> { throw new NotFoundException("domain not found") }

        catalogItemRepository.getByIdInDomain(catalogItemId, existingDomain) >> catalogItem
        catalogItemRepository.getByIdInDomain(_, _) >> { throw new NotFoundException("item not found") }
    }

    def "retrieve a catalog item"() {
        when:
        def output = usecase.execute(new GetCatalogItemUseCase.InputData(catalogItemId, existingDomainId, existingClient), noRestrictionExistingClient)

        then:
        output.catalogItem == catalogItem
    }

    def "delegates item not found exception"() {
        when:
        usecase.execute(new GetCatalogItemUseCase.InputData(UUID.randomUUID(), existingDomainId, existingClient), noRestrictionExistingClient)

        then:
        thrown(NotFoundException)
    }

    def "delegates domain not found exception"() {
        when:
        usecase.execute(new GetCatalogItemUseCase.InputData(catalogItemId, UUID.randomUUID(), existingClient), noRestrictionExistingClient)

        then:
        thrown(NotFoundException)
    }

    def "delegates not found exception for wrong client"() {
        given:
        def otherClientId = UUID.randomUUID()
        def otherClient = Mock(Client) { id >> otherClientId }

        when:
        usecase.execute(new GetCatalogItemUseCase.InputData(catalogItemId, existingDomainId, otherClient), NoRestrictionAccessRight.from(otherClientId.toString()))

        then:
        thrown(NotFoundException)
    }
}
