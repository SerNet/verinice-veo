/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Alexander Koderman
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
package org.veo.rest.test

import java.time.Instant

import org.veo.core.usecase.unit.CreateDemoUnitUseCase

import groovy.util.logging.Slf4j

@Slf4j
class UnitRestTestITSpec extends VeoRestTest {

    public static final String UNIT_NAME = 'Testunit'

    public static final String NEW_UNIT_NAME = "New and improved unit name"

    def "Creating a unit returns the correct HTTP status code"() {
        expect:
        def response = post("/units", [
            name: UNIT_NAME
        ], 201)
    }

    def "Select demo unit"() {
        given: 'a unit and a client'
        post("/units", [
            name: UNIT_NAME
        ], 201)
        when: "retrieving the client's units"
        def units = get("/units").body
        log.info("==> units:  {}", units)
        then: 'the demo unit is included'
        def demoUnit = units.find { it.name == CreateDemoUnitUseCase.DEMO_UNIT_NAME }
        demoUnit != null
        when: 'retrieving the demo unit from the backend'
        def unit = getUnit(demoUnit.id)
        log.info("==> unit:  {}", unit)
        then: 'the demo unit is returned'
        unit != null
        unit.name == CreateDemoUnitUseCase.DEMO_UNIT_NAME
    }


    def "Create a unit"() {
        def user = defaultUserName

        when: "a unit is created"
        def beforeCreation = Instant.now()
        def postResponse = postNewUnit(UNIT_NAME)

        then: "the operation succeeds"
        with(postResponse) {
            success
            message == "Unit created successfully."
            resourceId != null
        }

        when: "the unit is requested"
        def getResponse = getUnit(postResponse.resourceId)

        then: "the stored unit is retrieved"

        with(getResponse) {
            id == postResponse.resourceId
            name == UNIT_NAME
            createdBy == user
            updatedBy == user
            domains.size > 0
            Instant.parse(it.createdAt) > beforeCreation
            Instant.parse(it.updatedAt) > beforeCreation
        }
    }

    def "Update a unit"() {
        def user = defaultUserName

        given: "a unit"
        def postResponseBody = postNewUnit(UNIT_NAME)
        def getResponse = get("/units/${postResponseBody.resourceId}")

        when: "the unit is updated (roundtrip test)"
        // Use the response as the next request:
        def putRequestBody = getResponse.body
        putRequestBody.name = NEW_UNIT_NAME

        // Carry over the ETag:
        def etagHeader = getResponse.headers.get('ETag').toString()

        def beforeUpdate = Instant.now()
        put("/units/${postResponseBody.resourceId}", putRequestBody, etagHeader)

        and: "the same unit id is retrieved"
        def changedResponse = getUnit(postResponseBody.resourceId)

        then: "the response reflects the change"
        with(changedResponse) {
            id == postResponseBody.resourceId
            name == NEW_UNIT_NAME
            createdBy == user
            updatedBy == user
            domains.size > 0
            Instant.parse(createdAt) > beforeCreation
            Instant.parse(createdAt) < beforeUpdate
            Instant.parse(updatedAt) > beforeUpdate
            updatedAt != createdAt
        }
    }

    def "Delete a unit"() {
        given: "a unit"
        def postResponse = postNewUnit(UNIT_NAME)
        def getResponse = getUnit(postResponse.resourceId)

        when: "the unit is deleted"
        restDeleteUnit(postResponse.resourceId)

        and: "it is requested again"
        def deletedResponse = get("/units/${postResponse.resourceId}", 404).body

        then: "it can no longer be retrieved"
        with(deletedResponse) {
            success == false
            resourceId == null
            message == postResponse.resourceId
        }
    }

    private void restDeleteUnit(id) {
        delete("/units/${id}")
    }
}