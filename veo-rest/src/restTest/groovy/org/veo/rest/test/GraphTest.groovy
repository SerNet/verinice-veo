/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Alina Tsikunova
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

import org.springframework.http.MediaType

class GraphTestRestTest extends VeoRestTest {

    def "the relations endpoint returns a valid structure"() {
        given: "domain with two processes and one relation"
        def unitUri = post("/units", [
            name: "GraphTest Unit",
            domains: [
                [targetUri: "/domains/$testDomainId"],
            ]
        ]).location

        def assetId = post("/domains/$testDomainId/assets", [
            name: "Important asset B",
            owner: [targetUri: unitUri],
            subType: "Information",
            status: "CURRENT"
        ]).body.resourceId

        def processSubId = post("/domains/$testDomainId/processes", [
            name: "Important SUB process vom process A",
            owner: [targetUri: unitUri],
            subType: "BusinessProcess",
            status: "NEW"
        ]).body.resourceId

        def processId = post("/domains/$testDomainId/processes", [
            name: "Important process A",
            owner: [targetUri: unitUri],
            subType: "BusinessProcess",
            status: "NEW",
            links: [necessaryData: [
                    [target: [targetUri: "/assets/$assetId"]]
                ]],
            parts: [
                [targetUri: "/processes/$processSubId"]
            ]
        ]).body.resourceId

        def scopeId = post("/domains/$testDomainId/scopes", [
            name: "Main Scope",
            subType: 'Company',
            status: 'NEW',
            owner: [targetUri: unitUri],
            members: [
                [targetUri: "/processes/$processId"]
            ]
        ]).body.resourceId

        when: "the graph endpoint is called"
        def response = get(
                "/domains/$testDomainId/processes/$processId/relations",
                200,
                UserType.DEFAULT,
                MediaType.APPLICATION_JSON,
                ["Accept-Language": lang]
                )

        def nodes = response.body.nodes
        def links = response.body.links

        then: "the response contains nodes and links"
        nodes instanceof List
        links instanceof List
        !nodes.isEmpty()
        !links.isEmpty()

        and: "all expected nodes are included"
        def displayNames = nodes*.displayName as Set
        displayNames.any { it.endsWith("Important process A") }
        displayNames.any { it.endsWith("Important SUB process vom process A") }
        displayNames.any { it.endsWith("Important asset B") }
        displayNames.any { it.endsWith("Main Scope") }

        and: "all nodes use the designator format PRO-123 Name"
        nodes.each { node ->
            node.displayName ==~ /[A-Z]{3}-\d+ .+/
        }

        and: "all nodes have valid required fields"
        nodes.every { it.id && it.displayName && it.elementType && it.elementSubType }

        and: "nodes have correct subTypeName"
        nodes.find { it.elementId == processId }.elementSubType == "BusinessProcess"
        nodes.find { it.elementId == processSubId }.elementSubType == "BusinessProcess"
        nodes.find { it.elementId == assetId }.elementSubType == "Information"
        nodes.find { it.elementId == scopeId }.elementSubType == "Company"

        and: "the customlinks relation is present and correct"
        def customLink = links.find {
            it.type == "CUSTOM_LINK" && (it.source.endsWith(processId) && it.target.endsWith(assetId))
        }
        customLink.label == expectedCustomLinksLabel

        and: "the part_or_members relation is present and correct"
        def pm = links.findAll { it.type == "PART_OR_MEMBER" }
        def partMember_ProcessSub = pm.find{
            it.source.endsWith(processId) && it.target.endsWith(processSubId)
        }
        partMember_ProcessSub.label == expectedPartMemberLabel

        def partMember_ScopeProcess = pm.find{
            it.source.endsWith(scopeId) && it.target.endsWith(processId)
        }
        partMember_ScopeProcess.label == expectedPartMemberLabel

        where:
        lang | expectedCustomLinksLabel | expectedPartMemberLabel
        "de" | "notwendige Daten"       | "enthält"
        "en" | "necessary data"         | "contains"
    }
    def "returns 404 for non-existent element ID"() {
        given:
        def nonExistElementId = UUID.randomUUID()

        when:
        def response = get(
                "/domains/$testDomainId/processes/$nonExistElementId/relations",
                404,
                UserType.DEFAULT,
                MediaType.APPLICATION_JSON,
                )

        then:
        response.statusCode == 404
    }

    def "returns 404 when accessing an element belonging to another client"() {
        given: "asset created inside the test domain"
        def unitUri = post("/units", [
            name: "Unit 404",
            domains: [
                [targetUri: "/domains/$testDomainId"]
            ]
        ]).location

        def assetId = post(
                "/domains/$testDomainId/assets",[
                    name: "Asset 404 ",
                    owner: [targetUri: unitUri],
                    subType: "Information",
                    status: "CURRENT"
                ]
                ).body.resourceId

        when: "the secondary client tries to fetch relations of that asset"
        def response = get(
                "/domains/$testDomainId/assets/$assetId/relations",
                404,
                UserType.SECONDARY_CLIENT_USER
                )

        then: "the domain is hidden as if it does not exist"
        response.body.message == "domain $testDomainId not found"
    }

    def "referenced element only appears once even if referenced twice"() {
        given: "process A is referenced twice (customlink + scope-member)"
        def unitUri = post("/units", [
            name: "Unit 200",
            domains: [
                [targetUri: "/domains/$testDomainId"]
            ]
        ]).location

        def assetId = post("/domains/$testDomainId/assets", [
            name: "Asset B",
            owner: [targetUri: unitUri],
            subType: "Information",
            status: "CURRENT"
        ]).body.resourceId

        def processId = post("/domains/$testDomainId/processes", [
            name: "Process A",
            owner: [targetUri: unitUri],
            subType: "BusinessProcess",
            status: "NEW",
            links: [necessaryData: [
                    [target: [targetUri: "/assets/$assetId"]]
                ]]
        ]).body.resourceId

        def scopeId = post("/domains/$testDomainId/scopes", [
            name: "Mixed Scope",
            subType: 'Company',
            status: 'NEW',
            owner: [targetUri: unitUri],
            members: [
                [targetUri: "/processes/$processId"],
            ]
        ])

        when: "the graph endpoint is called"
        def response = get(
                "/domains/$testDomainId/processes/$processId/relations",
                200,
                UserType.DEFAULT,
                MediaType.APPLICATION_JSON
                )

        then: "process A only appears once in nodes"
        def processNode = response.body.nodes.findAll { it.elementId == processId }
        processNode.size() == 1

        and: "process A has two links (customlink + scope-member)"
        response.body.links.size() == 2
    }
}