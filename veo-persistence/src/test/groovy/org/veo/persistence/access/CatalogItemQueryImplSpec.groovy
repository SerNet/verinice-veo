/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Jonas Jordan.
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

import org.springframework.beans.factory.annotation.Autowired

import org.veo.core.entity.Domain
import org.veo.core.entity.Key
import org.veo.core.repository.CatalogItemQuery
import org.veo.core.repository.PagingConfiguration
import org.veo.core.repository.QueryCondition
import org.veo.persistence.access.jpa.CatalogItemDataRepository
import org.veo.persistence.access.jpa.ClientDataRepository
import org.veo.persistence.access.query.CatalogItemQueryImpl
import org.veo.persistence.entity.jpa.AbstractJpaSpec
import org.veo.persistence.entity.jpa.ClientData

class CatalogItemQueryImplSpec extends AbstractJpaSpec {
    @Autowired
    ClientDataRepository clientDataRepository
    @Autowired
    CatalogItemDataRepository repo

    ClientData client
    Domain domain
    CatalogItemQuery query

    def setup() {
        client = clientDataRepository.save(newClient{
            id = Key.newUuid()
            newDomain(it)
        })
        domain = client.domains.first()
        query = new CatalogItemQueryImpl(repo, domain)
    }

    def 'queries by domain'() {
        given:
        repo.saveAll([
            newCatalogItem(domain) {
                name = "my CI 1"
                elementType = "asset"
                subType = "server"
                status = "up"
            },
            newCatalogItem(domain) {
                name = "my CI 2"
                elementType = "asset"
                subType = "server"
                status = "up"
            }
        ])

        and:
        newDomain(client) {
            newCatalogItem(it) {
                name = "other CI"
                elementType = "asset"
                subType = "server"
                status = "up"
            }
        }
        clientDataRepository.save(client)

        when:
        def result = query.execute(PagingConfiguration.UNPAGED)

        then:
        result.totalResults == 2
        result.resultPage*.name == ["my CI 1", "my CI 2"]
    }

    def 'queries by element type & sub type'() {
        given:
        repo.saveAll([
            newCatalogItem(domain) {
                name = "abigail"
                elementType = "person"
                subType = "philosopher"
                status = "alive"
            },
            newCatalogItem(domain) {
                name = "clemens"
                elementType = "person"
                subType = "doctor"
                status = "alive"
            },
            newCatalogItem(domain) {
                name = "earthQuake"
                elementType = "incident"
                subType = "naturalDisaster"
                status = "old"
            },
        ])

        expect:
        query.execute(PagingConfiguration.UNPAGED)
                .resultPage*.name ==~ [
                    "abigail",
                    "clemens",
                    "earthQuake"
                ]

        when:
        query.whereElementTypeMatches(new QueryCondition<>(Set.of("person")))

        then:
        query.execute(PagingConfiguration.UNPAGED)
                .resultPage*.name ==~ ["abigail", "clemens"]

        when:
        query.whereSubTypeMatches(new QueryCondition<>(Set.of("philosopher")))

        then:
        query.execute(PagingConfiguration.UNPAGED)
                .resultPage*.name ==~ ["abigail"]
    }
}