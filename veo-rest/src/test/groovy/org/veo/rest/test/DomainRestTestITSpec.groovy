/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Urs Zeidler.
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

import static org.veo.rest.test.UserType.CONTENT_CREATOR

import org.apache.http.HttpStatus

import spock.util.concurrent.PollingConditions

class DomainRestTestITSpec extends VeoRestTest {

    def setup() {
        postNewUnit("some unit")
    }

    def "export the test domain"() {
        when: "the catalog is retrieved"
        def dsgvoId = getDomains().find { it.name == "test-domain" }.id
        def domainDto = exportDomain(dsgvoId)

        then: "the domain is exported"
        with(domainDto.catalogs[0]) {
            catalogItems.size() == 6
            name == "TEST-Controls"
            domainTemplate.displayName == "td test-domain"
        }
    }

    def "export the dsgvo domain"() {
        when: "the catalog is retrieved"
        def dsgvoId = getDomains().find { it.name == "DS-GVO" }.id
        def domainDto = exportDomain(dsgvoId)

        def catalog = domainDto.catalogs[0]
        def vvt = catalog.catalogItems.find { it.element.abbreviation == "VVT" }
        def tomi = catalog.catalogItems.find { it.element.abbreviation == "TOM-I" }
        def dsg23 = catalog.catalogItems.find { it.element.abbreviation == "DS-G.23" }

        then: "the domain is exported"
        with(catalog) {
            catalogItems.size() == 65
            name == "DS-GVO"
        }
        with (vvt) {
            namespace == "TOM.VVT"
            tailoringReferences.size() == 8
            with (element) {
                description == "VVT-Prozess"
                domains[dsgvoId].subType == "PRO_DataProcessing"
                domains[dsgvoId].status == "NEW"
            }
        }
        with (tomi.element) {
            customAspects.size() == 1
            customAspects.control_dataProtection.attributes.size() == 1
            customAspects.control_dataProtection.attributes.control_dataProtection_objectives == [
                "control_dataProtection_objectives_integrity"
            ]
            domains[dsgvoId].subType == "CTL_TOM"
            domains[dsgvoId].status == "NEW"
        }
        with (dsg23) {
            with (element) {
                name == "Keine Widerspruchsmöglichkeit für Betroffene gegen die Datenverarbeitung"
                domains[dsgvoId].subType == "SCN_Scenario"
                domains[dsgvoId].status == "NEW"
            }
        }
    }

    def "create a new domain template version with a profile"() {
        given: "a DS-GVO domain"
        def oldDomain = getDomains().find { it.name == "DS-GVO" }

        when: "we create a new version of the domain with a profile"
        def profileSourceUnitId = postNewUnit("the unit formerly known as demo unit").resourceId
        post("/assets", [
            name: "example asset",
            domains: [
                (oldDomain.id): [
                    subType: "AST_Datatype",
                    status: "NEW"
                ]
            ],
            owner: [targetUri: "$baseUrl/units/$profileSourceUnitId"],
        ]).body.resourceId
        def newTemplateVersionId = post("/domains/${oldDomain.id}/createdomaintemplate", [
            version: "1.4.1",
            profiles: ["exampleUnit": (profileSourceUnitId)]
        ], 201, CONTENT_CREATOR).body.targetUri.split('/').last()
        delete("/units/$profileSourceUnitId")

        and: "we create a domain from the new template version"
        post("/domaintemplates/${newTemplateVersionId}/createdomains", [:], HttpStatus.SC_NO_CONTENT, UserType.ADMIN)
        new PollingConditions().within(5) {
            getDomains().count { it.name == oldDomain.name } == 2
        }
        def newDomainId = getDomains().find { it.name == oldDomain.name && it.createdAt > oldDomain.createdAt }.id

        and: "the profile is applied to a new unit"
        def profileTargetUnitId = post("/units", [name: "apply profile here!"]).body.resourceId
        post("/domains/$newDomainId/profiles/exampleUnit/units/$profileTargetUnitId", null, 204)

        then: "the profile elements have been added to the unit"
        with(get("/assets?unit=$profileTargetUnitId").body.items) {
            size() == 1
            first().name == "example asset"
            first().domains[newDomainId].subType == "AST_Datatype"
        }
    }
}