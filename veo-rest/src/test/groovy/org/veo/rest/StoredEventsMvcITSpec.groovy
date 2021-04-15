/*******************************************************************************
 * Copyright (c) 2021 Jonas Jordan.
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
import org.springframework.context.annotation.ComponentScan
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.security.test.context.support.WithUserDetails

import org.veo.core.VeoMvcSpec
import org.veo.core.entity.Key
import org.veo.core.entity.Unit
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.StoredEventRepository
import org.veo.persistence.access.UnitRepositoryImpl
import org.veo.rest.configuration.WebMvcSecurityConfiguration

/**
 * Integration test to verify entity event generation. Performs operations on the REST API and performs assertions on the {@link StoredEventRepository}.
 */
@SpringBootTest(
webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
classes = [WebMvcSecurityConfiguration]
)
@EnableAsync
@ComponentScan(["org.veo.rest","org.veo.rest.configuration"])
class StoredEventsMvcITSpec extends VeoMvcSpec {

    @Autowired
    private ClientRepositoryImpl clientRepository

    @Autowired
    private UnitRepositoryImpl unitRepository

    @Autowired
    private StoredEventRepository storedEventRepository

    private Unit unit
    private Key clientId = Key.uuidFrom(WebMvcSecurityConfiguration.TESTCLIENT_UUID)

    def setup() {
        txTemplate.execute {
            def client = clientRepository.save(newClient {
                id = clientId
            })

            unit = newUnit(client) {
                name = "Test unit"
            }

            clientRepository.save(client)
            unitRepository.save(unit)
        }
    }

    @WithUserDetails("user@domain.example")
    def "document events are generated"() {
        when: "creating a document"
        String documentId = parseJson(post("/documents", [
            name: "doc",
            owner: [
                targetUri: "/units/${unit.id.uuidValue()}"
            ]
        ])).resourceId

        then: "a CREATION event is stored"
        with(getLatestStoredEventContent()) {
            type == "CREATION"
            url == "/documents/$documentId"
            author == "user@domain.example"
            with(content) {
                id == documentId
                name == "doc"
            }
        }

        when: "updating the document"
        def eTag = getETag(get("/documents/$documentId"))
        put("/documents/$documentId", [
            name: "super doc",
            owner: [
                targetUri: "/units/${unit.id.uuidValue()}"
            ]
        ], ["If-Match": eTag])

        then: "a MODIFICATION event is stored"
        with(getLatestStoredEventContent()) {
            type == "MODIFICATION"
            url == "/documents/$documentId"
            author == "user@domain.example"
            with(content) {
                id == documentId
                name == "super doc"
            }
        }

        when: "deleting the document"
        delete("/documents/$documentId")

        then: "a HARD_DELETION event is stored"
        with(getLatestStoredEventContent()) {
            type == "HARD_DELETION"
            url == "/documents/$documentId"
            author == "user@domain.example"
            content == null
        }
    }

    private Object getLatestStoredEventContent() {
        parseJson(storedEventRepository.findAll().sort { it.id }.last().content)
    }
}
