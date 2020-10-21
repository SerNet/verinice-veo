/*******************************************************************************
 * Copyright (c) 2020 Jochen Kemnade.
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
package org.veo.rest

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.transaction.support.TransactionTemplate

import org.veo.core.VeoMvcSpec
import org.veo.core.entity.Key
import org.veo.core.usecase.repository.UnitRepository
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.rest.configuration.WebMvcSecurityConfiguration

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [WebMvcSecurityConfiguration])
@WithUserDetails("user@domain.example")
class LinkingMvcITSpec extends VeoMvcSpec {

    @Autowired
    ClientRepositoryImpl clientRepository

    @Autowired
    UnitRepository unitRepository

    @Autowired
    TransactionTemplate txTemplate

    String unitId

    def setup() {
        txTemplate.execute {
            def client = clientRepository.save(newClient {
                id = Key.uuidFrom(WebMvcSecurityConfiguration.TESTCLIENT_UUID)
            })
            unitId = unitRepository.save(newUnit(client)).id.uuidValue()
        }
    }

    def "save multiple links"() {
        given: "three persons"
        def person1 = parseJson(post("/persons", [
            name: "person 1",
            owner: [targetUri: "/units/$unitId"]
        ])).resourceId
        def person2 = parseJson(post("/persons", [
            name: "person 2",
            owner: [targetUri: "/units/$unitId"]
        ])).resourceId
        def person3 = parseJson(post("/persons", [
            name: "person 3",
            owner: [targetUri: "/units/$unitId"]
        ])).resourceId

        when: "creating an asset with different links to all persons"
        def assetId = parseJson(post("/assets", [
            links: [
                Asset_owner_Person: [
                    [
                        name: "link to person 1",
                        target: [targetUri: "/persons/$person1"]
                    ],
                    [
                        name: "link to person 2",
                        target: [targetUri: "/persons/$person2"]
                    ]
                ],
                Asset_consulted_Person: [
                    [
                        name: "link to person 2",
                        target: [targetUri: "/persons/$person2"]
                    ],
                    [
                        name: "link to person 3",
                        target: [targetUri: "/persons/$person3"]
                    ]
                ]
            ],
            name : "asset",
            owner: [targetUri: "/units/$unitId"]
        ])).resourceId
        def retrievedAsset = parseJson(get("/assets/$assetId"))

        then:
        with(retrievedAsset.links.Asset_owner_Person.sort {it.name}) {
            size() == 2
            it[0].target.targetUri == "http://localhost/persons/$person1"
            it[1].target.targetUri == "http://localhost/persons/$person2"
        }
        with(retrievedAsset.links.Asset_consulted_Person.sort{it.name}) {
            size() == 2
            it[0].target.targetUri == "http://localhost/persons/$person2"
            it[1].target.targetUri == "http://localhost/persons/$person3"
        }
    }
}
