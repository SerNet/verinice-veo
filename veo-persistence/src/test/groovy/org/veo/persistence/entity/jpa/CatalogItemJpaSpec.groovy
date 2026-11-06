/*
 * verinice.veo
 * Copyright (C) 2023  Jonas Jordan
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
 */
package org.veo.persistence.entity.jpa

import org.hibernate.Hibernate
import org.hibernate.proxy.HibernateProxy
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.support.TransactionTemplate

import org.veo.core.entity.Client
import org.veo.core.entity.Domain
import org.veo.core.entity.ElementType
import org.veo.core.entity.TailoringReferenceType
import org.veo.persistence.access.jpa.CatalogItemDataRepository
import org.veo.persistence.access.jpa.ClientDataRepository
import org.veo.persistence.access.jpa.DomainDataRepository

import jakarta.persistence.EntityManager
import jakarta.validation.Validation

class CatalogItemJpaSpec extends AbstractJpaSpec{
    @Autowired
    ClientDataRepository clientRepository
    @Autowired
    DomainDataRepository domainRepository
    @Autowired
    CatalogItemDataRepository catalogItemRepository
    @Autowired
    TransactionTemplate txTemplate
    @Autowired
    EntityManager em

    Client client
    Domain domain

    def setup() {
        client = clientRepository.save(newClient() {
            domains = [newDomain(it)]
        })
        domain = client.domains.first()
    }

    def "can persist profile #type tailoring references"(TailoringReferenceType type) {
        given:
        def catalogItemSymIds = txTemplate.execute {
            def item1 = newCatalogItem(domain) {
                name = "target"
                elementType = ElementType.ASSET
                subType = "assetoBalsamico"
                status = "delicious"
            }
            def item2 = newCatalogItem(domain) {
                name = "source"
                elementType = ElementType.ASSET
                subType = "assetoBalsamico"
                status = "delicious"
                tailoringReferences = [
                    newTailoringReference(it, item1, type)
                ]
            }
            catalogItemRepository.saveAll([
                item1,
                item2
            ]
            )*.symbolicId
        }

        when:
        def items = catalogItemRepository.findAllByIdsAndDomain(catalogItemSymIds, domain.id, client.id)

        then:
        items.size() == 2
        with(items.find { it.name == "source" }.tailoringReferences[0]) {
            target.name == "target"
            referenceType == type
        }

        where:
        type << TailoringReferenceType.values()
    }

    def "an initialized catalog item proxy passes validation"() {
        given: "a persisted catalog item that is only known as a reference"
        def itemId = catalogItemRepository.save(newCatalogItem(domain) {
            name = "item"
            elementType = ElementType.ASSET
            subType = "assetoBalsamico"
            status = "delicious"
        }).id
        em.flush()
        em.clear()
        def proxy = em.getReference(CatalogItemData, itemId)
        Hibernate.initialize(proxy)

        expect:
        proxy instanceof HibernateProxy
        Validation.buildDefaultValidatorFactory().validator.validate(proxy).empty
    }
}
