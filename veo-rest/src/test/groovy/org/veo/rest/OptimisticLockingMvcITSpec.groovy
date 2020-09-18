/*******************************************************************************
 * Copyright (c) 2019 Alexander Koderman.
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

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

import org.spockframework.runtime.ConditionNotSatisfiedError
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.ComponentScan
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.security.test.context.support.WithUserDetails

import org.veo.core.entity.Domain
import org.veo.core.entity.Key
import org.veo.core.entity.Unit
import org.veo.core.usecase.common.ETag
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.UnitRepositoryImpl
import org.veo.persistence.entity.jpa.transformer.EntityDataFactory
import org.veo.rest.configuration.WebMvcSecurityConfiguration

import groovy.json.JsonSlurper

/**
 * Integration test for the unit controller. Uses mocked spring MVC environment.
 * Uses JPA repositories with in-memory database.
 * Does not start an embedded server.
 * Uses a test Web-MVC configuration with example accounts and clients.
 */
@SpringBootTest(
webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
classes = [WebMvcSecurityConfiguration]
)
@EnableAsync
@ComponentScan(["org.veo.rest","org.veo.rest.configuration"])
class OptimisticLockingMvcITSpec extends VeoRestMvcSpec {

    @Autowired
    private ClientRepositoryImpl clientRepository

    @Autowired
    private UnitRepositoryImpl unitRepository

    @Autowired
    private EntityDataFactory entityFactory

    private Unit unit
    private Unit unit2
    private Domain domain
    private Domain domain1
    private Key clientId = Key.uuidFrom(WebMvcSecurityConfiguration.TESTCLIENT_UUID)
    String salt = "d0eH%vC!l8"

    def setup() {
        txTemplate.execute {
            domain = entityFactory.createDomain()
            domain.description = "ISO/IEC"
            domain.abbreviation = "ISO"
            domain.name = "ISO"
            domain.id = Key.newUuid()

            domain1 = entityFactory.createDomain()
            domain1.description = "ISO/IEC2"
            domain1.abbreviation = "ISO"
            domain1.name = "ISO"
            domain1.id = Key.newUuid()

            def client= entityFactory.createClient()
            client.id = clientId
            client.domains = [domain, domain1] as Set

            unit = entityFactory.createUnit()
            unit.name = "Test unit"
            unit.id = Key.newUuid()

            unit.client = client

            clientRepository.save(client)
            unitRepository.save(unit)
        }
        ETag.setSalt(salt)
    }

    @WithUserDetails("user@domain.example")
    def "put an asset concurrently"() {
        given: "an asset"

        Map request = [
            name: 'E-Mail-Server',
            owner: [
                displayName: 'test2',
                targetUri: '/units/' + unit.id.uuidValue()
            ]
        ]
        def postResult = post('/assets', request)
        def postResultJson = new JsonSlurper().parseText(postResult.andReturn().response.contentAsString)

        and: "get the asset"

        def results = get("/assets/${postResultJson.resourceId}")
        String eTag = results.andReturn().response.getHeader("ETag")

        when: "put the asset"
        Map headers = [
            'If-Match': getTextBetweenQuotes(eTag)
        ]
        Map putRequest1 = [
            name: 'E-Mail-Server Berlin',
            owner: [
                displayName: 'test2',
                targetUri: '/units/' + unit.id.uuidValue()
            ]
        ]
        def resultPut1 = put("/assets/${postResultJson.resourceId}", putRequest1, headers)
        and: "put the asset again"
        Map putRequest2 = [
            name: 'E-Mail-Server Hamburg',
            owner: [
                displayName: 'test2',
                targetUri: '/units/' + unit.id.uuidValue()
            ]
        ]
        def resultPut2 = put("/assets/${postResultJson.resourceId}", putRequest2, headers)
        then: "a status code 200 returned for the first put"
        resultPut1.andExpect(status().isOk())
        and: "a ConditionNotSatisfiedError after the second put"
        thrown ConditionNotSatisfiedError
    }
}
