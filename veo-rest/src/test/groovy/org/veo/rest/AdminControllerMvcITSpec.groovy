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

import java.nio.charset.StandardCharsets

import org.springframework.security.test.context.support.WithAnonymousUser
import org.springframework.security.test.context.support.WithUserDetails

import org.veo.core.entity.specification.NotAllowedException
import org.veo.jobs.UserSwitcher

import groovy.util.logging.Log

@WithUserDetails("admin")
@Log
class AdminControllerMvcITSpec extends ContentSpec {

    public static final String DSGVO_NAME = "DS-GVO DS-GVO"

    def "generates unit dump"() {
        given: "a unit with a bunch of elements and risks"
        def client = createTestClient()
        createTestDomain(client, TEST_DOMAIN_TEMPLATE_ID)
        createTestDomain(client, DSGVO_TEST_DOMAIN_TEMPLATE_ID)
        def domainId = parseJson(get("/domains")).find { it.name == "DSGVO-test" }.id
        def (unitId, assetId, scenarioId, processId) = createUnitWithElements(UUID.fromString(domainId), true)

        when: "requesting a unit dump"
        def dump = parseJson(get("/admin/unit-dump/$unitId"))

        then: "it contains the unit and all its elements"
        with(dump) {
            unit.name == "you knit"
            domains.size() == 1
            elements*.type.sort() == [
                "asset",
                "control",
                "document",
                "incident",
                "person",
                "process",
                "scenario",
                "scope"
            ]
            risks*._self.sort() == [
                "http://localhost/assets/$assetId/risks/$scenarioId",
                "http://localhost/processes/$processId/risks/$scenarioId"
            ]
        }
    }

    def "update client domains"() {
        given: "a unit with a bunch of elements and risks"
        def client = createTestClient()
        createTestDomain(client, DSGVO_DOMAINTEMPLATE_UUID)
        createTestDomain(client, DSGVO_DOMAINTEMPLATE_V2_UUID)
        def domainId = parseJson(get("/domains")).find { it.templateVersion == "1.4.0" }.id
        def (unitId, assetId, scenarioId, processId) = createUnitWithElements(UUID.fromString(domainId), true, true)

        when: "the process risk values are preserved before migration"
        def json = parseJson(get("/admin/unit-dump/$unitId"))
        def risk = json.risks.find { it.process != null }
        def oldDomainName = risk.domains.(domainId).reference.displayName
        def oldRiskValues = risk.domains.(domainId).riskDefinitions.DSRA.riskValues
        def oldImpactValues = risk.domains.(domainId).riskDefinitions.DSRA.impactValues
        def oldProbability = risk.domains.(domainId).riskDefinitions.DSRA.probability

        and: 'updating all clients'
        post("/admin/domain-templates/${DSGVO_DOMAINTEMPLATE_V2_UUID}/allclientsupdate", [:], 204)

        then: 'the elements and risks are transferred to the new domain'
        defaultPolling.eventually {
            with(parseJson(get("/admin/unit-dump/$unitId"))) {
                domains.size() == 1
                domains.first().templateVersion == '2.0.0'
                def newDomainId = domains.first().id
                elements.size() == 8
                elements.each {
                    assert it.domains.keySet() =~ [newDomainId]
                    it.customAspects.each { type, ca ->
                        assert ca.domains*.targetUri =~ [
                            "http://localhost/domains/$domainId"
                        ]
                    }
                    it.links.each { type, linksOfType ->
                        linksOfType.each {
                            assert it.domains*.targetUri =~ [
                                "http://localhost/domains/$domainId"
                            ]
                        }
                    }
                }
                // process is present with risk values:
                with(elements.find { it.id == processId }) {
                    name == "process"
                    domains.(newDomainId).riskValues.DSRA.potentialImpacts.size() == 2
                    domains.(newDomainId).riskValues.DSRA.potentialImpacts.C == 0
                    domains.(newDomainId).riskValues.DSRA.potentialImpacts.I == 1
                }

                // scenario is present with risk values:
                with(elements.find { it.id == scenarioId }) {
                    name == "scenario"
                    domains.(newDomainId).riskValues.DSRA.potentialProbability == 2
                }

                // asset risk is present without risk values:
                with(risks.find { it.asset != null }) {
                    domains.size() == 1
                    domains.(newDomainId) != null
                    domains.(newDomainId).reference.displayName == AdminControllerMvcITSpec.DSGVO_NAME
                    domains.(newDomainId).riskDefinitions.size() == 0
                }

                // process risk is present with risk values:
                with(risks.find { it.process != null }) {
                    domains.size() == 1
                    domains.(newDomainId) != null

                    domains.(newDomainId).reference.displayName == oldDomainName
                    domains.(newDomainId).riskDefinitions.DSRA.riskValues ==~ oldRiskValues
                    domains.(newDomainId).riskDefinitions.DSRA.impactValues ==~ oldImpactValues
                    domains.(newDomainId).riskDefinitions.DSRA.probability == oldProbability
                }
            }
        }
    }

    def "create system message with tags"() {
        when:
        def result = parseJson(post("/admin/messages", [message:[DE: "test message"], level: 'INFO', tags: ['foo', 'bar']]))

        then:
        result == [success:true, resourceId:'1', message:'SystemMessage created successfully.']

        when:
        result = parseJson(get("/messages/1"))

        then:
        with(result) {
            tags ==~ ['foo', 'bar']
        }
    }

    @WithUserDetails("user@domain.example")
    def "regular user with correct API key can write system message"() {
        when:
        def result = parseJson(post("/admin/messages", [message:[DE: "test message"], level: 'INFO'], ['x-api-key': 'hello']))

        then:
        result.message == 'SystemMessage created successfully.'
        def id = result.resourceId

        when:
        result = parseJson(put("/admin/messages/$id", [message:[DE: "test message"], level: 'WARNING'], ['x-api-key': 'hello']))

        then:
        result.message == 'SystemMessage updated.'

        when:
        delete("/admin/messages/$id", ['x-api-key': 'hello'])

        then:
        noExceptionThrown()
    }

    @WithUserDetails("user@domain.example")
    def "regular user with wrong API key cannot write system message"() {
        when:
        post("/admin/messages", [message:[DE: "test message"], level: 'INFO'], ['x-api-key': 'invalid'], 403)

        then:
        thrown(NotAllowedException)

        when:
        put("/admin/messages/123", [message:[DE: "test message"], level: 'INFO'], ['x-api-key': 'invalid'], 403)

        then:
        thrown(NotAllowedException)

        when:
        delete("/admin/messages/123", ['x-api-key': 'invalid'], 403)

        then:
        thrown(NotAllowedException)
    }

    @WithUserDetails("user@domain.example")
    def "regular user without api key cannot write system message"() {
        when:
        post("/admin/messages", [message:[DE: "test message"], level: 'INFO'], 403)

        then:
        thrown(NotAllowedException)

        when:
        put("/admin/messages/123", [message:[DE: "test message"], level: 'INFO'], 403)

        then:
        thrown(NotAllowedException)

        when:
        delete("/admin/messages/123", 403)

        then:
        thrown(NotAllowedException)
    }

    @WithAnonymousUser
    def "unauthenticated user with correct api key can write system message"() {
        when:
        def result = parseJson(post("/admin/messages", [message:[DE: "test message"], level: 'INFO'], ['x-api-key': 'hello']))

        then:
        result.success == true
    }

    @WithAnonymousUser
    def "unauthenticated user without api key cannot write system message"() {
        when:
        post("/admin/messages", [message:[DE: "test message"], level: 'INFO'], 403)

        then:
        thrown(NotAllowedException)

        when:
        put("/admin/messages/123", [message:[DE: "test message"], level: 'INFO'], 403)

        then:
        thrown(NotAllowedException)

        when:
        delete("/admin/messages/123", 403)

        then:
        thrown(NotAllowedException)
    }

    @WithUserDetails("user@domain.example")
    def "regular user with correct API key can query unit count"() {
        given:
        def client = createTestClient()
        unitDataRepository.save(newUnit(client))

        when:
        def result = get("/admin/unit-count", ['x-api-key': 'dracula']).andReturn().response.getContentAsString(StandardCharsets.UTF_8)

        then:
        result == '1'

        when:
        def client2 = createTestClient()
        unitDataRepository.save(newUnit(client2))

        result = get("/admin/unit-count", ['x-api-key': 'dracula']).andReturn().response.getContentAsString(StandardCharsets.UTF_8)

        then:
        result == '2'
    }

    @WithUserDetails("user@domain.example")
    def "regular user with wrong API key cannot query unit count"() {
        when:
        get("/admin/unit-count", ['x-api-key': 'invalid'], 403)

        then:
        thrown(NotAllowedException)
    }

    @WithUserDetails("user@domain.example")
    def "regular user without api key cannot query unit count"() {
        when:
        get("/admin/unit-count", 403)

        then:
        thrown(NotAllowedException)
    }

    @WithAnonymousUser
    def "unauthenticated user with correct api key can query unit count"() {
        given:
        new UserSwitcher().runAsAdmin {
            createTestClient().tap {
                unitDataRepository.save(newUnit(it))
            }
        }

        when:
        def result = get("/admin/unit-count", ['x-api-key': 'dracula']).andReturn().response.getContentAsString(StandardCharsets.UTF_8)

        then:
        result == '1'
    }

    @WithAnonymousUser
    def "unauthenticated user without api key cannot query unit count"() {
        when:
        get("/admin/unit-count", 403)

        then:
        thrown(NotAllowedException)
    }
}
