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
import org.veo.core.entity.Key
import org.veo.core.entity.exception.NotFoundException
import org.veo.core.usecase.UseCaseSpec

class GetCatalogItemUseCaseSpec extends UseCaseSpec {

    CatalogItem catalogItem = Mock()

    Key existingDomainId = Key.newUuid()
    Key catalogId = Key.newUuid()
    Key catalogItemId = Key.newUuid()

    GetCatalogItemUseCase usecase = new GetCatalogItemUseCase()

    def setup() {
        existingDomain.getId() >> existingDomainId
        existingDomain.owner >> existingClient

        catalogItem.getId() >> catalogItemId
        catalogItem.getDomainBase() >> existingDomain
        existingDomain.getCatalogItems() >> [catalogItem]
        anotherClient.getDomains() >> []
    }

    def "retrieve a catalogitem"() {
        when:
        existingDomain.isActive() >> true
        def output = usecase.execute(new GetCatalogItemUseCase.InputData(catalogItemId, Optional.of(existingDomainId), existingClient))

        then:
        output.catalogItem != null
        output.catalogItem.id == catalogItemId
    }

    def "retrieve a catalogitem for a deleted domain"() {
        when:
        existingDomain.isActive() >> false
        usecase.execute(new GetCatalogItemUseCase.InputData(catalogItemId, Optional.of(existingDomainId), existingClient))

        then:
        thrown(NotFoundException)
    }

    def "retrieve a catalogitem for another client"() {
        when:
        existingDomain.isActive() >> true
        usecase.execute(new GetCatalogItemUseCase.InputData(catalogItemId, Optional.empty(), anotherClient))

        then:
        thrown(NotFoundException)
    }

    def "retrieve an unknown catalogitem"() {
        when:
        existingDomain.isActive() >> true
        usecase.execute(new GetCatalogItemUseCase.InputData(Key.newUuid(), Optional.of(existingDomainId), existingClient))

        then:
        thrown(NotFoundException)
    }
}
