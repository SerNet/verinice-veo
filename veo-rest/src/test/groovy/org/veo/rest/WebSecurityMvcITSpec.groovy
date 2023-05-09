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

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders

import org.veo.core.VeoMvcSpec
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.rest.security.WebSecurity

class WebSecurityMvcITSpec extends VeoMvcSpec {

    @Autowired
    private ClientRepositoryImpl clientRepository

    private String domainId

    static def USER_EDITABLE_PATHS = WebSecurity.USER_EDITABLE_PATHS.collect {
        it.replace("/**", "")
    }

    static def CONTENT_CREATOR_EDITABLE_PATHS = WebSecurity.CONTENT_CREATOR_EDITABLE_PATHS.collect {
        it.replace("/**", "")
    }

    def setup() {
        domainId = createTestDomain(createTestClient(), TEST_DOMAIN_TEMPLATE_ID).idAsString
    }

    def "unauthenticated requests fail"() {
        expect:
        mvc.perform(MockMvcRequestBuilders.get("/units"))
                .andReturn().response.status == 401
    }

    @WithUserDetails("user@domain.example")
    def "authenticated requests succeed"() {
        expect:
        get("/units").andReturn().response.status == 200
    }

    def "meta endpoints are unprotected"() {
        expect:
        mvc.perform(MockMvcRequestBuilders.get("/actuator"))
                .andReturn().response.status == 200
        mvc.perform(MockMvcRequestBuilders.get("/swagger-ui/index.html"))
                .andReturn().response.status == 200
    }

    @WithUserDetails("user@domain.example")
    def "admin endpoints are forbidden for a normal user"() {
        given: "a unit"
        def unitId = parseJson(post("/units", [name: "my little unit"])).resourceId

        expect: "unit dump to be forbidden"
        mvc.perform(MockMvcRequestBuilders
                .get("/admin/unit-dump/$unitId")).andReturn().response.status == 403

        and: "domain creation to be forbidden"
        mvc.perform(MockMvcRequestBuilders
                .post("/domaintemplates/f8ed22b1-b277-56ec-a2ce-0dbd94e24824/createdomains"))
                .andReturn().response.status == 403
    }

    @WithUserDetails("content-creator")
    def "domain template metadata is allowed for a normal user"() {
        expect:
        mvc.perform(MockMvcRequestBuilders
                .get("/domaintemplates")).andReturn().response.status == 200
    }

    @WithUserDetails("content-creator")
    def "domain template endpoints are allowed for a content-creator"() {
        expect: "domain template metadata to be allowed"
        mvc.perform(MockMvcRequestBuilders
                .get("/domaintemplates")).andReturn().response.status == 200

        and: "domain template import to be allowed"
        mvc.perform(MockMvcRequestBuilders
                .post("/domaintemplates")).andReturn().response.status == 400
        mvc.perform(MockMvcRequestBuilders
                .post("/domaintemplates")).andReturn().response.status == 400
    }

    @WithUserDetails("user@domain.example")
    def "content-creator endpoints are forbidden for a normal user"() {
        expect: "domain template creation to be forbidden"
        mvc.perform(MockMvcRequestBuilders
                .post("/domaintemplates")).andReturn().response.status == 403
        mvc.perform(MockMvcRequestBuilders
                .post("/content-creation/domains")).andReturn().response.status == 403
        mvc.perform(MockMvcRequestBuilders
                .post("/domaintemplates")).andReturn().response.status == 403
        mvc.perform(MockMvcRequestBuilders
                .get("/domaintemplates/" + TEST_DOMAIN_TEMPLATE_ID + "/export"))
                .andReturn().response.status == 403
    }

    @WithUserDetails("admin")
    def "content-creator endpoints are forbidden for an admin"() {
        expect: "domain template creation to be forbidden"
        mvc.perform(MockMvcRequestBuilders
                .post("/domaintemplates")).andReturn().response.status == 403
        mvc.perform(MockMvcRequestBuilders
                .post("/content-creation/domains")).andReturn().response.status == 403
        mvc.perform(MockMvcRequestBuilders
                .post("/domaintemplates")).andReturn().response.status == 403
        mvc.perform(MockMvcRequestBuilders
                .get("/domaintemplates/" + TEST_DOMAIN_TEMPLATE_ID + "/export"))
                .andReturn().response.status == 403
    }

    @WithUserDetails("read-only-user")
    def "user without write permission may GET #entity"() {
        given: "a client to retrieve #entity from"
        txTemplate.execute {
            def client = createTestClient()
            createTestDomain(client, DSGVO_TEST_DOMAIN_TEMPLATE_ID)
            clientRepository.save(client)
        }

        expect:
        switch (entity) {
            case "/schemas":
                assert mvc.perform(
                MockMvcRequestBuilders
                .get("/schemas/process?domains=$DSGVO_TEST_DOMAIN_TEMPLATE_ID"))
                .andReturn().response.status == 200
                break
            case "/translations":
                assert mvc.perform(
                MockMvcRequestBuilders
                .get("/translations?languages=en"))
                .andReturn().response.status == 200
                break
            case [
                "/domaintemplates",
            ]:
                assert mvc.perform(
                MockMvcRequestBuilders
                .get("/domaintemplates/$TEST_DOMAIN_TEMPLATE_ID"))
                .andReturn().response.status == 200
                break
            case ~/\/domains\/.+/:
                assert mvc.perform(
                MockMvcRequestBuilders
                .get(entity.replace("/domains", "/domains/$domainId")))
                .andReturn().response.status == 200
                break
            case "/content-creation":
                assert mvc.perform(MockMvcRequestBuilders.post(entity))
                .andReturn().response.status == 403
                break
            default:
                assert mvc.perform(MockMvcRequestBuilders.get(entity))
                .andReturn().response.status == 200
        }

        where:
        entity << USER_EDITABLE_PATHS + CONTENT_CREATOR_EDITABLE_PATHS
    }

    @WithUserDetails("no-rights-user")
    def "user without read permission may not GET #entity"() {
        expect:
        switch (entity) {
            case "/schemas":
                assert mvc.perform(MockMvcRequestBuilders
                .get("/schemas/process?domains=$DSGVO_TEST_DOMAIN_TEMPLATE_ID"))
                .andReturn().response.status == 403
                break
            case "/translations":
                assert mvc.perform(MockMvcRequestBuilders
                .get("/translations?languages=en"))
                .andReturn().response.status == 403
                break
            case [
                "/domaintemplates",
            ]:
                assert mvc.perform(MockMvcRequestBuilders
                .get("/domaintemplates/$TEST_DOMAIN_TEMPLATE_ID"))
                .andReturn().response.status == 403
                break
            default:
                assert mvc.perform(MockMvcRequestBuilders
                .get(entity)).andReturn().response.status == 403
        }

        where:
        entity << USER_EDITABLE_PATHS + CONTENT_CREATOR_EDITABLE_PATHS
    }

    @WithUserDetails("read-only-user")
    def "user without write access may not POST #entity"() {
        expect:
        mvc.perform(MockMvcRequestBuilders.post(entity))
                .andReturn().response.status == 403

        where:
        entity << USER_EDITABLE_PATHS + CONTENT_CREATOR_EDITABLE_PATHS
    }

    @WithUserDetails("read-only-user")
    def "user without write access may POST searches"() {
        expect:
        mvc.perform(MockMvcRequestBuilders.post("/processes/searches", [
            displayName: [
                values: ["can i haz enteetee plz?"]
            ]
        ])).andReturn().response.status == 400
    }

    @WithUserDetails("read-only-user")
    def "user without write access may POST evaluations"() {
        expect:
        // TODO VEO-1987 remove legacy endpoint call
        mvc.perform(MockMvcRequestBuilders.post("/processes/evaluation", [
            name: "can i haz evaluehshon?"
        ])).andReturn().response.status == 400
        mvc.perform(MockMvcRequestBuilders.post("/domains/$domainId/processes/evaluation", [
            name: "can i haz evaluehshon?"
        ])).andReturn().response.status == 400
    }

    @WithUserDetails("read-only-user")
    def "user without write access may not PUT #entity"() {
        expect:
        mvc.perform(MockMvcRequestBuilders.put(entity))
                .andReturn().response.status == 403

        where:
        entity << USER_EDITABLE_PATHS + CONTENT_CREATOR_EDITABLE_PATHS
    }

    @WithUserDetails("read-only-user")
    def "user without write access may not DELETE #entity"() {
        expect:
        mvc.perform(MockMvcRequestBuilders.delete(entity))
                .andReturn().response.status == 403

        where:
        entity << USER_EDITABLE_PATHS + CONTENT_CREATOR_EDITABLE_PATHS
    }

    @WithUserDetails("content-creator-readonly")
    def "content-creator without write access may only POST #entity if it is a domain(template)"() {
        expect:
        switch (entity) {
            case [
                "/domaintemplates",
            ]:
                assert mvc.perform(MockMvcRequestBuilders.post(entity, [
                    name: "can i haz dummytemplaid"
                ])).andReturn().response.status == 400
                break
            case ["/domains"]:
                assert mvc.perform(MockMvcRequestBuilders.post(
                "/content-creation/domains/$DSGVO_TEST_DOMAIN_TEMPLATE_ID/template"))
                .andReturn().response.status == 400
                break
            case [
                "/translations",
                "/catalogs",
                "/types"
            ]:
                assert mvc.perform(MockMvcRequestBuilders.post(entity))
                .andReturn().response.status == 405
                break
            case "/schemas":
            case "/content-creation":
                assert mvc.perform(MockMvcRequestBuilders.post(entity))
                .andReturn().response.status == 404
                break
            default:
                assert mvc.perform(MockMvcRequestBuilders.post(entity))
                .andReturn().response.status == 403
        }

        where:
        entity << USER_EDITABLE_PATHS + CONTENT_CREATOR_EDITABLE_PATHS
    }
}
