/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Jochen Kemnade.
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
package org.veo.core

import org.springframework.beans.factory.annotation.Autowired

import org.veo.core.entity.CatalogItem
import org.veo.core.entity.Client
import org.veo.core.entity.Control
import org.veo.persistence.access.CatalogItemRepositoryImpl
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.UnitRepositoryImpl
import org.veo.persistence.entity.jpa.CatalogItemData

import jakarta.transaction.Transactional
import jakarta.validation.ConstraintViolationException

@Transactional()
class CatalogItemRepositoryITSpec extends VeoSpringSpec {

    @Autowired
    private ClientRepositoryImpl clientRepository
    @Autowired
    private UnitRepositoryImpl unitRepository

    @Autowired
    private CatalogItemRepositoryImpl catalogItemRepository

    def "load a catalog item"() {
        given: "a client with a domain containing an item"
        Client client = newClient()
        def domain = newDomain(client) {domain->
            applyElementTypeDefinition(newElementTypeDefinition(Control.SINGULAR_TERM, domain) {
                subTypes = [
                    ctl : newSubTypeDefinition {
                        statuses = ["NEW"]
                    }
                ]
            })
        }
        newCatalogItem(domain) {
            elementType = Control.SINGULAR_TERM
            name = 'Control 1'
            subType = "ctl"
            status = "NEW"
        }
        client = clientRepository.save(client)
        def itemId = client.domains.first().catalogItems.first().id

        when:
        def item = catalogItemRepository.findById(itemId)

        then:
        item.present
        with( item.get() ) {
            name == 'Control 1'
            elementType == 'control'
        }
    }

    def "a catalog item cannot be saved without an associated element type"() {
        given: "a client with a catalog containing an item"
        def client = clientRepository.save(newClient())
        newDomain(client)
        client = clientRepository.save(client)
        def domain = client.domains.first()

        when:
        CatalogItem catalogItem = new CatalogItemData()
        catalogItem.setName("my name")
        catalogItem.setSubType("ctl")
        catalogItem.setStatus("NEW")
        catalogItem.setDomainBase(domain)
        catalogItemRepository.save(catalogItem)

        then:
        ConstraintViolationException ex = thrown(ConstraintViolationException)
        ex.getConstraintViolations().first().propertyPath ==~ /elementType/
        ex.getConstraintViolations().first().getMessageTemplate() ==~ /.*NotNull.message.*/
    }

    def "cascading relations are validated"() {
        given: "a client with a catalog containing an item"
        def client = clientRepository.save(newClient{
            newDomain(it)
        })
        def domain = client.domains.first()

        when:
        catalogItemRepository.save(newCatalogItem(domain, {
            newUpdateReference(it, null)
            newTailoringReference(it, null)
        }))

        then:
        def ex = thrown(ConstraintViolationException)
        ex.constraintViolations*.propertyPath*.toString().sort() == [
            "elementType",
            "status",
            "subType",
            "tailoringReferences[].referenceType",
            "updateReferences[].updateType",
        ]
    }
}
