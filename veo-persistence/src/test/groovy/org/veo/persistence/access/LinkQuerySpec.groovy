/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2024  Jonas Jordan
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
package org.veo.persistence.access

import static org.veo.core.entity.LinkDirection.INBOUND
import static org.veo.core.entity.LinkDirection.OUTBOUND
import static org.veo.core.repository.PagingConfiguration.SortOrder.ASCENDING
import static org.veo.core.repository.PagingConfiguration.SortOrder.DESCENDING

import org.springframework.beans.factory.annotation.Autowired

import org.veo.core.entity.Asset
import org.veo.core.entity.Domain
import org.veo.core.entity.Key
import org.veo.core.entity.Person
import org.veo.core.entity.Unit
import org.veo.core.repository.LinkQuery
import org.veo.core.repository.PagingConfiguration
import org.veo.persistence.access.jpa.AssetDataRepository
import org.veo.persistence.access.jpa.ClientDataRepository
import org.veo.persistence.access.jpa.ElementDataRepository
import org.veo.persistence.access.jpa.PersonDataRepository
import org.veo.persistence.access.jpa.UnitDataRepository
import org.veo.persistence.access.query.LinkQueryImpl
import org.veo.persistence.entity.jpa.AbstractJpaSpec
import org.veo.persistence.entity.jpa.ElementData

import jakarta.persistence.EntityManager

class LinkQuerySpec extends AbstractJpaSpec {
    @Autowired
    ClientDataRepository clientDataRepository
    @Autowired
    EntityManager em
    @Autowired
    UnitDataRepository unitRepository
    @Autowired
    ElementDataRepository<ElementData> elementDataRepository
    @Autowired
    AssetDataRepository assetDataRepository
    @Autowired
    PersonDataRepository personDataRepository

    Domain domain1
    Domain domain2
    Unit unit
    Asset asset1
    Asset asset2
    Person person1
    Person person2

    def setup() {
        def client = clientDataRepository.save(newClient {
            id = Key.newUuid()
            newDomain(it) {name = "d1"}
            newDomain(it) {name = "d2"}
        })
        unit = unitRepository.save(newUnit(client))
        domain1 = client.domains.find{it.name == "d1"}
        domain2 = client.domains.find{it.name == "d2"}

        asset1 = assetDataRepository.save(newAsset(unit).tap {
            name = "application server"
            abbreviation = "a1"
            associateWithDomain(domain1, "asset", "NEW")
        })
        asset2 = assetDataRepository.save(newAsset(unit) {
            name = "db server"
            abbreviation = "a2"
        })
        person1 = personDataRepository.save(newPerson(unit) {
            name = "abraham"
            abbreviation = "p1"
            addLink(newCustomLink(asset1, "favServer", domain1))
        })
        person2 = personDataRepository.save(newPerson(unit) {
            name = "linda"
            abbreviation = "p2"
            addLink(newCustomLink(asset1, "leastFavServer", domain1))
        })
        asset1.addLink(newCustomLink(asset2, "requires", domain1))
    }

    def 'links can be queried'() {
        given:
        def query = new LinkQueryImpl(em, elementDataRepository, asset1, domain1)

        expect:
        with(query.execute(new PagingConfiguration<>(10, 0, LinkQuery.SortCriterion.LINKED_ELEMENT_NAME, ASCENDING))) {
            totalResults == 3
            totalPages == 1

            it.resultPage[0].linkedElement().name == "abraham"
            it.resultPage[0].linkType() == "favServer"
            it.resultPage[0].direction() == INBOUND

            it.resultPage[1].linkedElement().name == "db server"
            it.resultPage[1].linkType() == "requires"
            it.resultPage[1].direction() == OUTBOUND

            it.resultPage[2].linkedElement().name == "linda"
            it.resultPage[2].linkType() == "leastFavServer"
            it.resultPage[2].direction() == INBOUND
        }

        and: "other sort options work"
        with(query.execute(new PagingConfiguration<>(10, 0, LinkQuery.SortCriterion.LINKED_ELEMENT_ABBREVIATION, ASCENDING))) {
            resultPage*.linkedElement()*.abbreviation == ["a2", "p1", "p2"]
        }
        with(query.execute(new PagingConfiguration<>(10, 0, LinkQuery.SortCriterion.DIRECTION, ASCENDING))) {
            resultPage*.direction() == [INBOUND, INBOUND, OUTBOUND]
        }
        with(query.execute(new PagingConfiguration<>(10, 0, LinkQuery.SortCriterion.DIRECTION, DESCENDING))) {
            resultPage*.direction() == [OUTBOUND, INBOUND, INBOUND]
        }

        and: "pagination works"
        with(query.execute(new PagingConfiguration<>(2, 0, LinkQuery.SortCriterion.LINKED_ELEMENT_NAME, ASCENDING))) {
            totalResults == 3
            totalPages == 2
            resultPage*.linkedElement()*.name == ["abraham", "db server"]
        }
        with(query.execute(new PagingConfiguration<>(2, 1, LinkQuery.SortCriterion.LINKED_ELEMENT_NAME, ASCENDING))) {
            totalResults == 3
            totalPages == 2
            resultPage*.linkedElement()*.name == ["linda"]
        }
    }

    def "filters by domain"() {
        given:
        assetDataRepository.save(newAsset(unit) {
            name = "domain 2 asset"
            addLink(newCustomLink(asset1, "domain2Link", domain2))
        })
        def domain2Scope = scopeDataRepository.save(newScope(unit) {
            name = "domain 2 scope"
        })
        asset1.addLink(newCustomLink(domain2Scope, "domain2Link", domain2))

        when:
        def domain2Query = new LinkQueryImpl(em, elementDataRepository, asset1, domain2)

        then:
        with(domain2Query.execute(new PagingConfiguration<>(10, 0, LinkQuery.SortCriterion.LINKED_ELEMENT_NAME, ASCENDING))) {
            resultPage*.linkedElement()*.name == [
                "domain 2 asset",
                "domain 2 scope"
            ]
        }

        when:
        def domain1Query = new LinkQueryImpl(em, elementDataRepository, asset1, domain1)

        then:
        with(domain1Query.execute(new PagingConfiguration<>(10, 0, LinkQuery.SortCriterion.LINKED_ELEMENT_NAME, ASCENDING))) {
            resultPage*.linkedElement()*.name == [
                "abraham",
                "db server",
                "linda"
            ]
        }
    }
}