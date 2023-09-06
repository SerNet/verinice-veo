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

import static java.util.UUID.randomUUID
import static org.veo.rest.test.UserType.CONTENT_CREATOR

import org.apache.http.HttpStatus
import org.springframework.security.test.context.support.WithUserDetails

import org.veo.adapter.service.domaintemplate.DomainTemplateIdGeneratorImpl
import org.veo.core.entity.Process

import groovy.util.logging.Slf4j

@Slf4j
class DomainRestTestITSpec extends DomainRestTest {

    def "export the test domain"() {
        when: "the domain is retrieved"
        def domainDto = exportDomain(testDomainId)

        then: "catalog items are present"
        domainDto.catalogItems.size() == 7
    }

    def "export the dsgvo domain"() {
        when: "the domain is exported"
        def domainDto = exportDomain(dsgvoDomainId)

        def vvt = domainDto.catalogItems.find { it.abbreviation == "VVT" }
        def tomi = domainDto.catalogItems.find { it.abbreviation == "TOM-I" }
        def dsg23 = domainDto.catalogItems.find { it.abbreviation == "DS-G.23" }

        then: "the domain is exported"
        domainDto.catalogItems.size() == 65
        with (vvt) {
            namespace == "TOM.VVT"
            tailoringReferences.size() == 8
            description == "VVT-Prozess"
            subType == "PRO_DataProcessing"
            status == "NEW"
        }
        with (tomi) {
            customAspects.size() == 1
            customAspects.control_dataProtection.size() == 1
            customAspects.control_dataProtection.control_dataProtection_objectives == [
                "control_dataProtection_objectives_integrity"
            ]
            subType == "CTL_TOM"
            status == "NEW"
        }
        with(dsg23) {
            name == "Keine Widerspruchsmöglichkeit für Betroffene gegen die Datenverarbeitung"
            subType == "SCN_Scenario"
            status == "NEW"
        }
    }

    def "post a domaintemplate with translation errors"() {
        given: "a DS-GVO domaintemplate with translation errors"
        def domainDto = exportDomain(dsgvoDomainId)
        def modifiedDomain = [:] << domainDto

        modifiedDomain.name = "Broken DSGVO_${UUID.randomUUID()}"
        modifiedDomain.templateVersion = '6.6.6'
        modifiedDomain.id = (new DomainTemplateIdGeneratorImpl()).createDomainTemplateId(
                modifiedDomain.name,
                modifiedDomain.templateVersion
                )
        modifiedDomain.elementTypeDefinitions.process.translations.de.remove('process_controller')
        modifiedDomain.elementTypeDefinitions.process.translations.de.superfluous_key = "I'm not even supposed to be here today!"

        when: " we post the domaintemplate"
        def response = post("/domaintemplates", modifiedDomain, 422, UserType.CONTENT_CREATOR)

        then: "the errors are recognized"
        response.getStatusCode() == 422
        response.body ==~ /.*Issues were found in the translations: Language 'de': MISSING: process_controller ; SUPERFLUOUS: superfluous_key.*/
    }

    def "create a new domain template version with a profile"() {
        given: "a DS-GVO domain"
        def oldDomain = get("/domains/$dsgvoDomainId").body

        when: "we create a unit with elements and a risk"
        def profileSourceUnitId = postNewUnit().resourceId
        def processId = post("/processes", [
            name   : "example process",
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
        def newTemplateVersionId = post("/content-creation/domains/${oldDomain.id}/template", [
            version: "1.4.1",
            profiles: [
                exampleUnit: [
                    unitId: profileSourceUnitId,
                    name: 'Example profile',
                    description: 'An example profile',
                    language: 'en_US'
                ]
            ]
        ], 201, CONTENT_CREATOR).body.targetUri.split('/').last()
        delete("/units/$profileSourceUnitId")

        and: "we create a domain from the new template version"
        post("/domaintemplates/${newTemplateVersionId}/createdomains", [:], HttpStatus.SC_NO_CONTENT, UserType.ADMIN)
        defaultPolling.eventually {
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
        with(domainDto.profiles.exampleUnit) {
            name == 'Example profile'
            elements*.name ==~ [
                "example process",
                "example scenario"
            ]
            risks.size() == 1
        }

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
            profiles.exampleUnit.elements[0].domains.keySet() ==~ [domainTemplate.id]
        }

        when:" we post the domain export as domaintemplate"
        domainDto.name = "My own domain"

        def template = post("/domaintemplates", domainDto, 201, UserType.CONTENT_CREATOR).body
        def templateId = template.resourceId
        def domainTemplateExport = get("/domaintemplates/$templateId/export", 200, UserType.CONTENT_CREATOR).body

        then: "the domaintemplate exist and contains the data"
        with(domainTemplateExport) {
            name == domainDto.name
            templateVersion == domainDto.templateVersion
            profiles.exampleUnit.elements*.name ==~ [
                "example process",
                "example scenario"
            ]
            profiles.exampleUnit.risks.size() == 1
        }
    }

    @WithUserDetails("user@domain.example")
    def "retrieve sub type statistic for the stored DS-GVO catalog items"() {
        given:
        def dsgvoId = getDomains().find { it.name == "DS-GVO" }.id

        when:
        def result = get("/domains/${dsgvoId}/catalog-items/type-count").body

        then:
        result.size() == 3
        with(result.process) {
            PRO_DataProcessing == 1
        }
        with(result.scenario) {
            SCN_Scenario == 56
        }
        with(result.control) {
            CTL_TOM == 8
        }
    }

    def "create a new catalog from a unit for a new domain"() {
        given: "a new domain"
        def domainName = "catalogitem creation test ${randomUUID()}"
        def domainId = post("/content-creation/domains", [
            name: domainName,
            abbreviation: "dct",
            description: "best one ever",
            authority: "ME",
        ], 201, CONTENT_CREATOR).body.resourceId

        postPersonObjectSchema(domainId)
        postProcessObjectSchema(domainId)
        postAssetObjectSchema(domainId)

        when: "we create a unit with elements"
        def catalogSourceUnitId = postNewUnit().resourceId
        def person1Id = post("/domains/$domainId/persons", [
            name   : "example person 1",
            subType: "PER_Person",
            status: "NEW",
            owner: [targetUri: "/units/$catalogSourceUnitId"],
        ]).body.resourceId

        post("/domains/$domainId/persons", [
            name: "example person 2",
            subType: "PER_Person",
            status: "NEW",
            owner: [targetUri: "$baseUrl/units/$catalogSourceUnitId"],
            parts: [
                [targetUri: "/persons/$person1Id"],
            ]
        ])

        post("/domains/$domainId/assets", [
            name: "example asset",
            subType: "server",
            status: "off",
            owner: [targetUri: "$baseUrl/units/$catalogSourceUnitId"],
        ])

        post("/domains/$domainId/processes", [
            name: "example process",
            subType: "PRO_Process",
            status: "NEW",
            owner: [targetUri: "$baseUrl/units/$catalogSourceUnitId"]
        ])

        and: "create new catatlog items from the unit"
        put("/content-creation/domains/${domainId}/catalog-items?unit=$catalogSourceUnitId",
                [:], null, 204, CONTENT_CREATOR)
        def catalogItems = get("/catalogs/${domainId}/items").body

        then:
        catalogItems.size() == 4

        when: "we incarnet all items"
        def unitId = postNewUnit().resourceId

        def catalogItemsIds = catalogItems.collect{it.id}.join(',')
        def incarnationDescription = get("/units/${unitId}/incarnations?itemIds=${catalogItemsIds}").body

        then:
        with(incarnationDescription.parameters.find { it.item.displayName == "example person 1" } ) {
            references.size() ==1
            references.first().referenceType == "COMPOSITE"
        }

        with(incarnationDescription.parameters.find { it.item.displayName == "example person 2" } ) {
            references.size() ==1
            references.first().referenceType == "PART"
        }

        when:
        def elements = post("/units/${unitId}/incarnations", incarnationDescription).body
        def persons = get("/persons?unit=${unitId}").body

        then: "the part relation is added"
        with(persons.items.find{ it.name == "example person 2" }) {
            parts.size() == 1
            parts.first().displayName.endsWith("example person 1")
        }

        when: "create new catatlog items from the unit"
        put("/content-creation/domains/${domainId}/catalog-items?unit=$unitId",
                [:], null, 204, CONTENT_CREATOR)

        and:
        catalogItems = get("/catalogs/${domainId}/items").body

        then:
        catalogItems != null
        with(catalogItems.sort{ it.name }) {
            size() == 4
            with(it.first()) {
                name == 'example asset'
            }
        }

        when: "export the domain"
        def exportedDomain = get("/domains/${domainId}/export").body

        then:
        exportedDomain !=null
        with(exportedDomain) {
            catalogItems.size() == 4
            with(catalogItems.find { it.name == "example person 2" }) {
                tailoringReferences.size() == 1
                tailoringReferences[0].referenceType == "PART"
                tailoringReferences[0].catalogItem.displayName == "example person 1"
            }
            with(catalogItems.find { it.name == "example person 1" }) {
                tailoringReferences.size() == 1
                tailoringReferences[0].referenceType == "COMPOSITE"
                tailoringReferences[0].catalogItem.displayName == "example person 2"
            }
        }
    }
}