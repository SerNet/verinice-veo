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

import org.veo.core.entity.EntityType
import org.veo.core.usecase.unit.CreateDemoUnitUseCase
import org.veo.rest.ControllerConstants

import groovy.util.logging.Slf4j
import spock.util.concurrent.PollingConditions

@Slf4j
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

        when: "we create a unit with elements and a risk"
        def profileSourceUnitId = postNewUnit("the unit formerly known as demo unit").resourceId
        def processId = post("/processes", [
            name: "example process",
            domains: [
                (oldDomain.id): [
                    subType: "PRO_DataProcessing",
                    status: "NEW"
                ]
            ],
            owner: [targetUri: "$baseUrl/units/$profileSourceUnitId"],
        ]).body.resourceId
        def scenarioId = post("/scenarios", [
            name: "example scenario",
            domains: [
                (oldDomain.id): [
                    subType: "SCN_Scenario",
                    status: "NEW"
                ]
            ],
            owner: [targetUri: "$baseUrl/units/$profileSourceUnitId"],
        ]).body.resourceId
        post("/processes/$processId/risks", [
            domains: [
                (oldDomain.id): [
                    reference: [targetUri: "$baseUrl/domains/${oldDomain.id}"],
                    riskDefinitions: [
                        DSRA: [
                            impactValues: [
                                [
                                    category: "A",
                                    specificImpact: 1
                                ]
                            ]
                        ]
                    ]
                ]
            ],
            scenario: [targetUri: "$baseUrl/scenarios/$scenarioId"],
        ])

        and: "create a new domain template with the a profile based on the unit"
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

        then: "the profile elements and risk have been added to the unit"
        with(get("/processes?unit=$profileTargetUnitId").body.items[0]) {
            name == "example process"
            domains[newDomainId].subType == "PRO_DataProcessing"
            def risk = owner.get(it._self + "/risks").body[0]
            risk.domains[newDomainId].riskDefinitions.DSRA.impactValues.find { it.category == "A" }.specificImpact == 1
        }
        with(get("/scenarios?unit=$profileTargetUnitId").body.items) {
            size() == 1
            first().name == "example scenario"
            first().domains[newDomainId].subType == "SCN_Scenario"
        }

        when: "We export the domain"
        def domainDto = exportDomain(newDomainId)

        then: "the profile is contained"
        domainDto.profiles.exampleUnit.elements*.name ==~ [
            "example process",
            "example scenario"
        ]
        domainDto.profiles.exampleUnit.risks.size() == 1

        when: "we export the new domain template"
        def domainTemplate = get("/domaintemplates/${newTemplateVersionId}/export").body

        then: "the domaintemplate is returned"
        with(domainTemplate) {
            name == "DS-GVO"
            templateVersion == "1.4.1"
            profiles.exampleUnit.elements*.name ==~ [
                "example process",
                "example scenario"
            ]
            profiles.exampleUnit.risks.size() == 1
        }
    }

    def "create a new domain template version with a profile from the demo unit"() {
        given: "a DS-GVO domain"
        def oldDomain = getDomains().find { it.name == "DS-GVO" }

        def units = get("/units").body
        def demoUnit = units.find { it.name == CreateDemoUnitUseCase.DEMO_UNIT_NAME }

        when: "creating a new domain template with a profile based on the demo unit"
        def newTemplateVersionId = post("/domains/${oldDomain.id}/createdomaintemplate", [
            version: "1.4.3",
            profiles: ["orgDemoUnit": (demoUnit.id)]
        ], 201, CONTENT_CREATOR).body.targetUri.split('/').last()

        and: "we create a domain from the new template version"
        post("/domaintemplates/${newTemplateVersionId}/createdomains", [:], HttpStatus.SC_NO_CONTENT, UserType.ADMIN)
        new PollingConditions().within(5) {
            getDomains().count { it.name == oldDomain.name } == 3
        }
        def newDomainId = getDomains().find { it.name == oldDomain.name && it.templateVersion == "1.4.3" }.id

        and: "the profile is applied to a new unit"
        def profileTargetUnitId = post("/units", [name: "apply profile here!"]).body.resourceId
        post("/domains/$newDomainId/profiles/orgDemoUnit/units/$profileTargetUnitId", null, 204)

        def elementTypes = EntityType.ELEMENT_TYPES*.pluralTerm

        then: "all profile elements have been applied"
        for (etype in elementTypes) {
            def oldObject = get("/${etype}?unit=${demoUnit.id}").body
            def newObject = get("/${etype}?unit=$profileTargetUnitId").body

            with(newObject) {
                items.size() == oldObject.items.size()
                for(def i=0;i<items.size();i++) {
                    with(items[i]) {
                        name == oldObject.items.get(i).name
                        description == oldObject.items[i].description
                        domains[newDomainId].subType == oldObject.items[i].domains[newDomainId].subType
                    }
                }
            }
        }
    }
}