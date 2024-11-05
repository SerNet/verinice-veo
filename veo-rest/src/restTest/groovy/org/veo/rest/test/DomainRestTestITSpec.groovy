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

        then: "catalog items are present"
        domainDto.catalogItems.size() == 65
        with (vvt) {
            name == "VVT"
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

        and: "risk definition is present"
        domainDto.riskDefinitions.DSRA.impactInheritingLinks.process.contains("process_requiredITSystems")
    }

    def "post a domain template with translation errors"() {
        given: "a DS-GVO domain template with translation errors"
        def domainDto = exportDomain(dsgvoDomainId)
        def modifiedDomain = [:] << domainDto

        modifiedDomain.name = "Broken DSGVO_${UUID.randomUUID()}"
        modifiedDomain.templateVersion = '6.6.6'
        modifiedDomain.elementTypeDefinitions.process.translations.de.remove('process_controller')
        modifiedDomain.elementTypeDefinitions.process.translations.de.superfluous_key = "I'm not even supposed to be here today!"

        when: " we post the domain template"
        def response = post("/content-creation/domain-templates", modifiedDomain, 422, UserType.CONTENT_CREATOR)

        then: "the errors are recognized"
        response.getStatusCode() == 422
        response.body ==~ /.*Issues were found in the translations: Language 'de': MISSING: process_controller ; SUPERFLUOUS: superfluous_key.*/
    }

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

    def "retrieve specific custom aspects for catalog items"() {
        given:
        def dsgvoId = getDomains().find { it.name == "DS-GVO" }.id

        when:"we select all catalogItems with custom aspects control_dataProtection included"
        def result = get("/domains/${dsgvoId}/catalog-items?customAspects=control_dataProtection&size=100").body

        then: "we find all items and some have the control_dataProtection customAspect"
        result.totalItemCount == 65
        with(result.items.findAll { !it.customAspects.isEmpty() }) {
            size() == 8
            customAspects.control_dataProtection.size() == 8
            customAspects.control_dataProtection.control_dataProtection_objectives.collect { it.first() }  ==~ [
                'control_dataProtection_objectives_resilience',
                'control_dataProtection_objectives_integrity',
                'control_dataProtection_objectives_availability',
                'control_dataProtection_objectives_confidentiality',
                'control_dataProtection_objectives_pseudonymization',
                'control_dataProtection_objectives_encryption',
                'control_dataProtection_objectives_recoverability',
                'control_dataProtection_objectives_effectiveness'
            ]
        }

        when:"we select without custom aspects"
        result = get("/domains/${dsgvoId}/catalog-items?size=100").body

        then: "we find all items and none have the control_dataProtection customAspect"
        result.totalItemCount == 65
        result.items.findAll { it.customAspects !== null }.empty
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

        putPersonDefinition(domainId)
        putProcessDefinition(domainId)
        putAssetDefinition(domainId)

        when: "we create a unit with elements"
        def catalogSourceUnitId = postNewUnit("U1", [domainId]).resourceId
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

        and: "create new catalog items from the unit"
        put("/content-creation/domains/${domainId}/catalog-items?unit=$catalogSourceUnitId",
                [:], null, 204, CONTENT_CREATOR)
        def catalogItems = get("/domains/${domainId}/catalog-items").body.items

        then:
        catalogItems.size() == 4

        when: "we incarnet all items"
        def unitId = postNewUnit("U1", [domainId]).resourceId

        def catalogItemsIds = catalogItems.collect{it.id}.join(',')
        def incarnationDescription = get("/units/${unitId}/domains/$domainId/incarnation-descriptions?itemIds=${catalogItemsIds}").body
        def incarnationDescriptionManual = get("/units/${unitId}/domains/$domainId/incarnation-descriptions?itemIds=${catalogItemsIds}&mode=MANUAL").body

        then: "manual mode and default mode must be the same because all were listed as itemIds"
        incarnationDescription == incarnationDescriptionManual

        and: "the elements are contained"
        with(incarnationDescription.parameters.find { it.item.displayName == "example person 1" } ) {
            references.size() ==1
            references.first().referenceType == "COMPOSITE"
        }

        with(incarnationDescription.parameters.find { it.item.displayName == "example person 2" } ) {
            references.size() ==1
            references.first().referenceType == "PART"
        }

        when:
        post("/units/${unitId}/incarnations", incarnationDescription)
        def persons = get("/persons?unit=${unitId}").body

        then: "the part relation is added"
        with(persons.items.find{ it.name == "example person 2" }) {
            parts.size() == 1
            parts.first().displayName.endsWith("example person 1")
        }

        when: "creating new catalog items from the unit"
        put("/content-creation/domains/${domainId}/catalog-items?unit=$unitId",
                [:], null, 204, CONTENT_CREATOR)

        and:
        catalogItems = get("/domains/${domainId}/catalog-items").body.items

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
        with(exportedDomain.catalogItems) {
            size() == 4
            with(it.find { it.name == "example person 2" }) {
                tailoringReferences.size() == 1
                tailoringReferences[0].referenceType == "PART"
                tailoringReferences[0].target.displayName == "example person 1"
            }
            with(it.find { it.name == "example person 1" }) {
                tailoringReferences.size() == 1
                tailoringReferences[0].referenceType == "COMPOSITE"
                tailoringReferences[0].target.displayName == "example person 2"
            }
        }
    }

    def "retrieve element type definition"() {
        when:
        def result = get("/domains/$dsgvoDomainId").body

        then:
        result.elementTypeDefinitions.process.translations.de.process_PRO_DataTransfer_plural == "Datenübertragungen"
    }

    def "existing catalog items can be modified"() {
        given: "a unit with two elements"
        def domainId = post("/content-creation/domains", [
            name: "catalog item update test ${randomUUID()}",
            authority: "JJ",
        ], 201, CONTENT_CREATOR).body.resourceId
        putPersonDefinition(domainId)
        def unitId = postNewUnit("U1", [domainId]).resourceId
        def person1Id = post("/domains/$domainId/persons", [
            name: "person 1",
            subType: "PER_Person",
            status: "NEW",
            owner: [targetUri: "/units/$unitId"]
        ]).body.resourceId
        def person2Id = post("/domains/$domainId/persons", [
            name: "person 2",
            subType: "PER_Person",
            status: "NEW",
            owner: [targetUri: "/units/$unitId"]
        ]).body.resourceId

        when: "creating catalog items from the unit"
        put("/content-creation/domains/$domainId/catalog-items?unit=$unitId", null, null, 204)
        def originalPerson1Item = get("/domains/$domainId/catalog-items").body.items.find { it.name == "person 1" }
        def originalPerson2Item = get("/domains/$domainId/catalog-items").body.items.find { it.name == "person 2" }

        and: "adding, updating and deleting source elements"
        get("/domains/$domainId/persons/$person1Id").with {
            body.name = "updated person 1"
            put(body._self, body, getETag(), 200)
        }
        delete("/persons/$person2Id")
        post("/domains/$domainId/persons", [
            name: "person 3",
            subType: "PER_Person",
            status: "NEW",
            owner: [targetUri: "/units/$unitId"]
        ]).body.resourceId

        and: "updating the catalog items"
        put("/content-creation/domains/$domainId/catalog-items?unit=$unitId", null, null, 204)

        then: "modifications have been applied"
        with(get("/domains/$domainId/catalog-items").body.items) {
            it.size() == 2
            with(it.find { it.name == "updated person 1" }) {
                id == originalPerson1Item.id
                updatedAt > originalPerson1Item.updatedAt
            }
            with(it.find { it.name == "person 3" }) {
                id != originalPerson1Item.id
                id != originalPerson2Item.id
            }
        }
    }
}