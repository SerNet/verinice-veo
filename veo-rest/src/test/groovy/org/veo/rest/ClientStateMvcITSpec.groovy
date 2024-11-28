/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Jonas Jordan.
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
package org.veo.rest

import static org.veo.core.entity.ClientState.ACTIVATED
import static org.veo.core.entity.ClientState.DEACTIVATED
import static org.veo.core.entity.event.ClientEvent.ClientChangeType.DEACTIVATION
import static org.veo.rest.configuration.WebMvcSecurityConfiguration.TESTCLIENT_UUID

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.test.context.support.WithUserDetails

import org.veo.core.VeoMvcSpec
import org.veo.core.repository.ClientRepository
import org.veo.rest.common.ClientNotActiveException

/**
 * Tests the unit controller's responses wrt. different client states
 */
class ClientStateMvcITSpec extends VeoMvcSpec {
    @Autowired
    ClientRepository clientRepository

    @WithUserDetails("user@domain.example")
    def "fetch data for a new client"() {
        given:
        createTestDomainTemplate(DSGVO_DOMAINTEMPLATE_UUID)
        createTestDomainTemplate(TEST_DOMAIN_TEMPLATE_ID)
        def client = createTestClient()
        executeInTransaction {
            defaultDomainCreator.addDomain(client, "ISO", false)
            clientRepository.save(client)
        }

        when: "we examine the client and the domain"
        client = clientRepository.findById(UUID.fromString(TESTCLIENT_UUID)).get()

        then: "the state is active"
        client.state == ACTIVATED

        when: "we get the units"
        def units = parseJson(get("/units"))

        then:
        noExceptionThrown()
        units.empty
    }

    @WithUserDetails("user@domain.example")
    def "deactivate the client"() {
        given:
        createTestDomainTemplate(DSGVO_DOMAINTEMPLATE_UUID)
        createTestDomainTemplate(TEST_DOMAIN_TEMPLATE_ID)
        def client = createTestClient()
        executeInTransaction {
            defaultDomainCreator.addDomain(client, "ISO", false)
            clientRepository.save(client)
        }

        when: "posting unit and asset as a new client"
        def unitId = parseJson(post("/units", ["name": "nova"])).resourceId
        post("/assets", [
            name: "New Asset",
            owner: [
                displayName: "test2",
                targetUri: "http://localhost/units/$unitId"
            ]
        ])

        then:"the asset exists"
        parseJson(get("/assets")).totalItemCount == 1

        when: "we deactivate the client"
        client = clientRepository.findById(UUID.fromString(TESTCLIENT_UUID)).get()
        client.updateState(DEACTIVATION)
        clientRepository.save(client)
        client = clientRepository.findById(UUID.fromString(TESTCLIENT_UUID)).get()

        then: "its state has been updated"
        client.state ==  DEACTIVATED

        when: "requesting units"
        get("/units",403)

        then: "an exception is thrown"
        thrown(ClientNotActiveException)

        when:"requesting assets"
        get("/assets",403)

        then:"an exception is thrown"
        thrown(ClientNotActiveException)
    }
}
