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
package org.veo.core.usecase.catalogitem

import org.veo.core.entity.Catalog
import org.veo.core.entity.CatalogItem
import org.veo.core.entity.Domain
import org.veo.core.entity.DomainTemplate
import org.veo.core.entity.Key
import org.veo.core.entity.exception.NotFoundException
import org.veo.core.usecase.UseCaseSpec
import org.veo.core.usecase.catalogitem.GetCatalogItemsUseCase.InputData

class GetCatalogItemsUseCaseSpec extends UseCaseSpec {

    Catalog catalog = Mock()
    DomainTemplate domaintemplate = Mock()
    Key existingDomainId = Key.newUuid()
    Key catalogId = Key.newUuid()
    Key catalog1Id = Key.newUuid()
    Key catalog2Id = Key.newUuid()

    GetCatalogItemsUseCase usecase = new GetCatalogItemsUseCase()

    def setup() {
        existingDomain.getId() >> existingDomainId
        existingDomain.owner >> existingClient
        existingDomain.active >> true

        catalog.getId() >> catalogId
        catalog.getDomainTemplate() >> existingDomain
        Catalog catalog2 = Mock()
        catalog2.getId() >> catalog2Id
        catalog2.getDomainTemplate() >> existingDomain
        catalog2.catalogItems >> [
            Mock(CatalogItem),
            Mock(CatalogItem)
        ]

        existingDomain.catalogs >> [catalog, catalog2]

        CatalogItem ci1 = Mock()
        ci1.namespace >> 'A.B.C'
        CatalogItem ci2 = Mock()
        ci2.namespace >> 'A.B.C.D'
        CatalogItem ci3 = Mock()
        ci3.namespace >> 'A.B.C.D'
        catalog.catalogItems >> [ci1, ci2, ci3]

        Domain domain = Mock()
        domain.active >> true

        Catalog catalog1 = Mock()
        catalog1.getId() >> catalog1Id
        catalog1.getDomainTemplate() >> domain
        catalog1.catalogItems >> [Mock(CatalogItem)]
        domain.catalogs >> [catalog1]

        anotherClient.getDomains() >> [existingDomain, domain]
    }

    def "retrieve all catalogitems for an unkown catalog"() {
        when:
        def output = usecase.execute(new InputData(Optional.empty(),Key.newUuid(),Optional.empty(),  anotherClient))
        then:
        thrown(NotFoundException)
    }

    def "retrieve all catalogitems for an other catalog"() {
        when:
        def output = usecase.execute(new InputData(Optional.empty(),catalog1Id,Optional.empty(),  existingClient))
        then:
        thrown(NotFoundException)
    }

    def "retrieve all catalogitems for a catalog"() {
        when:
        def output = usecase.execute(new InputData(Optional.empty(),catalogId,Optional.empty(),  anotherClient))
        then:
        output.catalogItems.size() == 3
    }

    def "retrieve all catalogitems for a namspace"() {
        when:
        def output = usecase.execute(new InputData(Optional.of('A.B.C.D'),catalogId,Optional.empty(),  anotherClient))
        then:
        output.catalogItems.size() == 2
        when:
        output = usecase.execute(new InputData(Optional.of('A.B.C'),catalogId,Optional.empty(),  anotherClient))
        then:
        output.catalogItems.size() == 1
        when:
        output = usecase.execute(new InputData(Optional.of('A.B.C.LL'),catalogId,Optional.empty(),  anotherClient))
        then:
        output.catalogItems.size() == 0
    }
}
