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

import static org.veo.core.entity.Client.ClientState.ACTIVATED
import static org.veo.core.entity.Client.ClientState.DEACTIVATED
import static org.veo.core.entity.event.ClientEvent.ClientChangeType.DEACTIVATION
import static org.veo.rest.configuration.WebMvcSecurityConfiguration.TESTCLIENT_UUID

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.test.context.support.WithUserDetails

import org.veo.core.VeoMvcSpec
import org.veo.core.entity.Client
import org.veo.core.entity.Key
import org.veo.core.repository.ClientRepository
import org.veo.core.usecase.unit.CreateDemoUnitUseCase
import org.veo.rest.common.ClientNotActiveException
/**
 * Tests the client creation process. Note that this one tests the Unit controller but doesn't have the setup where the
 * client is created manually.
 */
class ClientCreationMvcITSpec extends VeoMvcSpec {
    @Autowired
    ClientRepository clientRepository

    @WithUserDetails("user@domain.example")
    def "create a new client"() {
        given:
        createTestDomainTemplate(DSGVO_DOMAINTEMPLATE_UUID)
        createTestDomainTemplate(TEST_DOMAIN_TEMPLATE_ID)
        when: "posting a unit as a new client"
        post("/units", ["name": "nova"])

        then: "the client has been created"
        clientRepository.exists(Key.uuidFrom(TESTCLIENT_UUID))

        when: "we examine the client and the domain"
        Client client = clientRepository.findById(Key.uuidFrom(TESTCLIENT_UUID)).get()

        then: "the default domains are created"
        client.domains.size() == 2
        client.domains*.domainTemplate*.dbId.contains(DSGVO_DOMAINTEMPLATE_UUID)
        and: "the state is active"
        client.state == ACTIVATED

        when: "we get the units"
        def units = parseJson(get("/units"))
        def unitId = units[0].id

        then:"the demo unit is also created"
        units.size() == 2
        unitId != null
        units*.name.contains(CreateDemoUnitUseCase.DEMO_UNIT_NAME)
    }

    @WithUserDetails("user@domain.example")
    def "deactivate the client"() {
        given:
        createTestDomainTemplate(DSGVO_DOMAINTEMPLATE_UUID)
        createTestDomainTemplate(TEST_DOMAIN_TEMPLATE_ID)

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

        and: "the client has been created"
        clientRepository.exists(Key.uuidFrom(TESTCLIENT_UUID))

        when: "we examine the client and the domain"
        Client client = clientRepository.findById(Key.uuidFrom(TESTCLIENT_UUID)).get()

        then: "the default domains are created"
        client.domains*.domainTemplate*.dbId ==~ [
            DSGVO_DOMAINTEMPLATE_UUID,
            TEST_DOMAIN_TEMPLATE_ID
        ]

        when: "we deactivate the client"
        client.updateState(DEACTIVATION)
        clientRepository.save(client)
        client = clientRepository.findById(Key.uuidFrom(TESTCLIENT_UUID)).get()

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
