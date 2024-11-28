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
import org.veo.core.entity.exception.NotFoundException
import org.veo.core.usecase.UseCaseSpec
import org.veo.core.usecase.catalogitem.GetCatalogItemsUseCase.InputData

class GetCatalogItemsUseCaseSpec extends UseCaseSpec {

    def existingDomainId = UUID.randomUUID()
    GetCatalogItemsUseCase usecase = new GetCatalogItemsUseCase()

    def setup() {
        existingDomain.getId() >> existingDomainId
        existingDomain.owner >> existingClient
        existingDomain.active >> true

        CatalogItem ci1 = Mock()
        CatalogItem ci2 = Mock()
        CatalogItem ci3 = Mock()

        existingDomain.catalogItems >> [ci1, ci2, ci3]

        anotherClient.domains >> [existingDomain]
    }

    def "retrieve all catalog items for an unknown domain"() {
        when:
        usecase.execute(new InputData(UUID.randomUUID(), anotherClient))

        then:
        thrown(NotFoundException)
    }

    def "retrieve all catalog items for a domain"() {
        when:
        def output = usecase.execute(new InputData(existingDomainId, anotherClient))

        then:
        output.catalogItems.size() == 3
    }
}
