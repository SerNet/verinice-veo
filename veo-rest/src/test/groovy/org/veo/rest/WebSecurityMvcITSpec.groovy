/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Jonas Jordan
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders

import org.veo.core.VeoMvcSpec

class WebSecurityMvcITSpec extends VeoMvcSpec {
    def "unauthenticated requests fail"() {
        expect:
        mvc.perform(MockMvcRequestBuilders.get("/units")).andReturn().response.status == 401
    }

    @WithUserDetails("user@domain.example")
    def "authenticated requests succeed"() {
        expect:
        get("/units").andReturn().response.status == 200
    }

    def "meta endpoints are unprotected"() {
        expect:
        mvc.perform(MockMvcRequestBuilders.get("/actuator")).andReturn().response.status == 200
        mvc.perform(MockMvcRequestBuilders.get("/swagger-ui/index.html")).andReturn().response.status == 200
    }

    @WithUserDetails("user@domain.example")
    def "admin endpoints are forbidden for a normal user"() {
        given: "a unit"
        def unitId = parseJson(post("/units/", [name: "my little unit"])).resourceId
        expect: "unit dump to be forbidden"
        mvc.perform(MockMvcRequestBuilders.get("/admin/unit-dump/$unitId")).andReturn().response.status == 403
        and: "domain creation to be forbidden"
        mvc.perform(MockMvcRequestBuilders.post("/domaintemplates/f8ed22b1-b277-56ec-a2ce-0dbd94e24824/createdomains")).andReturn().response.status == 403
    }
}
