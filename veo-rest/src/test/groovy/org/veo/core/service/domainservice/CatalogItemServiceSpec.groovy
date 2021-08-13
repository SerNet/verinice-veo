/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Urs Zeidler.
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
package org.veo.core.service.domainservice

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.ComponentScan
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.test.context.ActiveProfiles

import org.veo.adapter.service.domaintemplate.CatalogItemServiceImpl
import org.veo.adapter.service.domaintemplate.DomainTemplateServiceImpl
import org.veo.core.VeoSpringSpec
import org.veo.core.entity.Client
import org.veo.core.entity.Domain
import org.veo.core.entity.EntityLayerSupertype
import org.veo.core.entity.Unit
import org.veo.core.repository.PagedResult
import org.veo.core.repository.PagingConfiguration
import org.veo.core.service.CatalogItemService
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.ControlRepositoryImpl
import org.veo.persistence.access.UnitRepositoryImpl


@SpringBootTest(classes = CatalogItemServiceSpec.class)
@WithUserDetails("user@domain.example")
class CatalogItemServiceSpec extends VeoSpringSpec {
    @Autowired
    private ClientRepositoryImpl repository

    @Autowired
    private UnitRepositoryImpl unitRepository

    @Autowired
    DomainTemplateServiceImpl domainTemplateService

    @Autowired
    CatalogItemService catalogItemService

    @Autowired
    private ControlRepositoryImpl repo

    private Client client
    private Unit unit;
    private Unit unit1;
    private Domain domainFromTemplate

    def item
    def element

    public setup () {
        client = repository.save(newClient {
            name = "Demo Client"
        })

        def domainsFromTemplate = null
        txTemplate.execute {
            domainsFromTemplate = domainTemplateService.createDefaultDomains(client)
            domainsFromTemplate.forEach({client.addToDomains(it)})
            client = repository.save(client)
            unit = unitRepository.save(newUnit(client) {
                name = "Test unit"
            })
        }
        domainFromTemplate = client.domains.first()
        item = domainFromTemplate.catalogs.first().catalogItems.sort({it.namespace}).first()
        element = catalogItemService.createInstance(item, domainFromTemplate)
    }

    def "create an element from a catalog item"() {
        given: "a client and a domain"
        expect: "the element is created and initialized"

        element.name == "Control-1"
        element.abbreviation == item.element.abbreviation
        element.description == item.element.description
        element.appliedCatalogItems.size() == 1
        element.appliedCatalogItems.contains(item)
        element.domains.size() == 1
        element.domains.contains(domainFromTemplate)

        when: "we take another item"

        item = domainFromTemplate.catalogs.first().catalogItems.sort({it.namespace})[2]
        element = catalogItemService.createInstance(item, domainFromTemplate)

        then: "the element is created and initialized"

        element.name == "Control-3"
        element.abbreviation == item.element.abbreviation
        element.description == item.element.description
        element.appliedCatalogItems.size() == 1
        element.appliedCatalogItems.contains(item)
        element.domains.size() == 1
        element.domains.contains(domainFromTemplate)
        element.links.size() == 1
        element.links.first().domains.contains(domainFromTemplate)
    }

    def "find an applied item"() {
        given: "a client"
        txTemplate.execute {
            unit1 = unitRepository.save(newUnit(client) {
                name = "Test unit1"
            })
        }
        expect: "the element is created and initalized"

        element.name == "Control-1"
        element.abbreviation == item.element.abbreviation
        element.description == item.element.description
        element.appliedCatalogItems.size() == 1
        element.appliedCatalogItems.contains(item)
        element.domains.size() == 1
        element.domains.contains(domainFromTemplate)

        when: "the element is saved and searched for by applieditems"
        element.setOwner(unit)
        element.setDesignator("CTL-1")
        repo.save(element)
        def result = repo.query(client).with {
            whereOwnerIs(unit)
            whereAppliedItemsContains(item)
            execute(PagingConfiguration.UNPAGED)
        }

        then:"the element is found"
        result.resultPage.size() == 1
        result.resultPage.first().appliedCatalogItems.contains(item)

        when: "searched in another unit"

        result = repo.query(client).with {
            whereOwnerIs(unit1)
            whereAppliedItemsContains(item)
            execute(PagingConfiguration.UNPAGED)
        }

        then:"the result is empty"
        result.resultPage.size() == 0
    }
}
