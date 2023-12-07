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

import org.springframework.security.test.context.support.WithUserDetails

import org.veo.core.VeoMvcSpec
import org.veo.rest.common.ClientNotActiveException

/**
 * Integration test for the controllers.
 *
 * The test checks that the methods for loading all objects in the
 * controllers work and returns HTTP status forbidden (403) if no client exists.
 *
 * Uses mocked spring MVC environment.
 * Uses JPA repositories with in-memory database.
 * Does not start an embedded server.
 * Uses a test Web-MVC configuration with example accounts and clients.
 */
class GetAllWithNoClientSpec extends VeoMvcSpec {

    def setup() {
    }

    @WithUserDetails("user@domain.example")
    def "retrieve all assets"() {
        when: "if the client was not created and request is made to the server"
        get("/assets", 403)

        then: "an exception is thrown"
        thrown(ClientNotActiveException)
    }

    @WithUserDetails("user@domain.example")
    def "retrieve all controls"() {
        when: "if the client was not created and request is made to the server"
        get("/controls", 403)

        then: "an exception is thrown"
        thrown(ClientNotActiveException)
    }

    @WithUserDetails("user@domain.example")
    def "retrieve all scopes"() {
        when: "if the client was not created and request is made to the server"
        get("/scopes", 403)

        then: "an exception is thrown"
        thrown(ClientNotActiveException)
    }

    @WithUserDetails("user@domain.example")
    def "retrieve all persons"() {
        when: "if the client was not created and request is made to the server"
        get("/persons", 403)

        then: "an exception is thrown"
        thrown(ClientNotActiveException)
    }

    @WithUserDetails("user@domain.example")
    def "retrieve all processes"() {
        when: "if the client was not created and request is made to the server"
        get("/processes", 403)

        then: "an exception is thrown"
        thrown(ClientNotActiveException)
    }

    @WithUserDetails("user@domain.example")
    def "retrieve all units"() {
        when: "if the client was not created and request is made to the server"
        get("/units", 403)

        then: "an exception is thrown"
        thrown(ClientNotActiveException)
    }
}
