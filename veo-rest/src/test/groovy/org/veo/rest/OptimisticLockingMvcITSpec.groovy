/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2019  Alexander Koderman.
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

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.test.context.support.WithUserDetails

import org.veo.core.VeoMvcSpec
import org.veo.core.entity.Domain
import org.veo.core.entity.Unit
import org.veo.core.usecase.common.ETagMismatchException
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.UnitRepositoryImpl

import groovy.json.JsonSlurper

/**
 * Integration test for the unit controller. Uses mocked spring MVC environment.
 * Uses JPA repositories with in-memory database.
 * Does not start an embedded server.
 * Uses a test Web-MVC configuration with example accounts and clients.
 */
class OptimisticLockingMvcITSpec extends VeoMvcSpec {

    @Autowired
    private ClientRepositoryImpl clientRepository

    @Autowired
    private UnitRepositoryImpl unitRepository

    private Unit unit
    private Domain domain

    def setup() {
        txTemplate.execute {
            def client = createTestClient()
            domain = createTestDomain(client, TEST_DOMAIN_TEMPLATE_ID, false)
            unit = unitRepository.save(newUnit(client) {
                name = "Test unit"
                addToDomains(domain)
            })
        }
    }

    @WithUserDetails("user@domain.example")
    def "put an asset concurrently"() {
        given: "an asset"
        Map request = [
            name: 'E-Mail-Server',
            subType: 'Server',
            status: 'RUNNING',
            owner: [
                targetUri: 'http://localhost/units/' + unit.idAsString
            ]
        ]
        def postResult = post("/domains/$domain.idAsString/assets", request)
        def postResultJson = new JsonSlurper().parseText(postResult.andReturn().response.contentAsString)

        and: "get the asset"
        def results = get("/assets/${postResultJson.resourceId}")
        String eTag = results.andReturn().response.getHeader("ETag")

        when: "putting the asset with current ETag"
        Map headers = [
            'If-Match': eTag
        ]
        put("/domains/$domain.idAsString/assets/${postResultJson.resourceId}", [
            name: 'E-Mail-Server Berlin',
            subType: 'Server',
            status: 'RUNNING',
            owner: [
                targetUri: 'http://localhost/units/' + unit.idAsString
            ]
        ], headers)

        then: "it was successful"
        noExceptionThrown()

        when: "putting the asset again with the same ETag"
        put("/domains/$domain.idAsString/assets/${postResultJson.resourceId}", [
            name: 'E-Mail-Server Hamburg',
            subType: 'Server',
            status: 'RUNNING',
            owner: [
                targetUri: 'http://localhost/units/' + unit.idAsString
            ]
        ], headers, 412)

        then: "a ETagMismatchException is thrown"
        thrown ETagMismatchException
    }
}
