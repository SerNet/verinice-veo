/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2025  Jochen Kemnade
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

import org.springframework.security.test.context.support.WithAnonymousUser
import org.springframework.security.test.context.support.WithUserDetails

import org.veo.core.entity.specification.NotAllowedException

class MessageControllerMvcITSpec extends ContentSpec {

    @WithUserDetails("user@domain.example")
    def "regular user key can read system message"() {
        when:
        def result = parseJson(get("/messages"))

        then:
        result == []
    }

    @WithUserDetails("user@domain.example")
    def "regular user with correct API key can read system message"() {
        when:
        def result = parseJson(get("/messages", ['x-api-key': 'hello']))

        then:
        result == []
    }

    @WithUserDetails("user@domain.example")
    def "regular user with wrong API key can read system message"() {
        when:
        def result = parseJson(get("/messages", ['x-api-key': 'invalid']))

        then:
        result == []
    }

    @WithAnonymousUser
    def "unauthenticated user with correct api key can read system message"() {
        when:
        def result = parseJson(get("/messages", ['x-api-key': 'hello']))

        then:
        result == []
    }

    @WithAnonymousUser
    def "unauthenticated user with wrong api key cannot read system message"() {
        when:
        get("/messages", ['x-api-key': 'invalid'], 403)

        then:
        thrown(NotAllowedException)
    }

    @WithAnonymousUser
    def "unauthenticated user without api key cannot read system message"() {
        when:
        get("/messages", 403)

        then:
        thrown(NotAllowedException)
    }
}
