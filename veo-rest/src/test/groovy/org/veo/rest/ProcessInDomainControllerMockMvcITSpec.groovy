/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Jonas Jordan
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

import static java.util.UUID.randomUUID

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.web.bind.MethodArgumentNotValidException

import org.veo.categories.MapGetProperties
import org.veo.core.VeoMvcSpec
import org.veo.core.entity.exception.NotFoundException
import org.veo.core.repository.ProcessRepository
import org.veo.core.repository.UnitRepository
import org.veo.core.usecase.common.ETag

import groovy.json.JsonSlurper
import spock.util.mop.Use

@WithUserDetails("user@domain.example")
class ProcessInDomainControllerMockMvcITSpec extends VeoMvcSpec {
    @Autowired
    private UnitRepository unitRepository
    @Autowired
    private ProcessRepository processRepository

    private String unitId
    private String testDomainId
    private String dsgvoTestDomainId

    def setup() {
        def client = createTestClient()
        testDomainId = createTestDomain(client, TEST_DOMAIN_TEMPLATE_ID).idAsString
        dsgvoTestDomainId = createTestDomain(client, DSGVO_TEST_DOMAIN_TEMPLATE_ID).idAsString
        client = clientRepository.getById(client.id)
        unitId = unitRepository.save(newUnit(client)).idAsString
    }

    def "CRUD process in domain contexts"() {
        given: "an process with linked process and a part"
        def assetId = parseJson(post("/domains/$testDomainId/assets", [
            name: "Market investigation results",
            owner: [targetUri: "/units/$unitId"],
            subType: "Information",
            status: "CURRENT",
        ])).resourceId
        def partId = parseJson(post("/domains/$testDomainId/processes", [
            name: "Promotion",
            owner: [targetUri: "/units/$unitId"],
            subType: "BusinessProcess",
            status: "NEW"
        ])).resourceId
        def processId = parseJson(post("/domains/$testDomainId/processes", [
            name: "Marketing",
            abbreviation: "M",
            description: "Catering to the target market",
            owner: [targetUri: "/units/$unitId"],
            subType: "BusinessProcess",
            status: "NEW",
            riskValues: [
                riskyDef: [
                    potentialImpacts: [
                        C: 1
                    ]
                ]
            ],
            customAspects: [
                general: [
                    complexity: "high"
                ]
            ],
            parts: [
                [ targetUri:"/processes/$partId" ]
            ],
            links: [
                necessaryData: [
                    [
                        target: [targetUri: "/assets/$assetId"],
                        attributes: [
                            essential: true
                        ]
                    ]
                ]
            ]
        ])).resourceId

        when: "fetching it in the domain context"
        def response = parseJson(get("/domains/$testDomainId/processes/$processId"))

        then: "basic properties are contained"
        response.id == processId
        response.type == "process"
        response._self == "http://localhost/domains/$testDomainId/processes/$processId"
        response.name == "Marketing"
        response.abbreviation == "M"
        response.description == "Catering to the target market"
        response.designator =~ /PRO-\d+/
        response.owner.targetUri == "http://localhost/units/$unitId"
        response.createdBy == "user@domain.example"
        response.createdAt != null
        response.updatedBy == "user@domain.example"
        response.updatedAt == response.createdAt

        and: "domain-specific properties"
        response.subType == "BusinessProcess"
        response.status == "NEW"
        response.customAspects.general.complexity == "high"
        response.links.necessaryData[0].target.targetUri == "http://localhost/assets/$assetId"
        response.links.necessaryData[0].target.targetInDomainUri == "http://localhost/domains/$testDomainId/assets/$assetId"
        response.links.necessaryData[0].target.associatedWithDomain
        response.links.necessaryData[0].target.subType == "Information"
        response.links.necessaryData[0].attributes.essential
        response.riskValues.riskyDef.potentialImpacts.C == 1

        and: "parts"
        response.parts[0].targetUri == "http://localhost/processes/$partId"
        response.parts[0].targetInDomainUri == "http://localhost/domains/$testDomainId/processes/$partId"
        response.parts[0].associatedWithDomain
        response.parts[0].subType == "BusinessProcess"

        and: "it conforms to the JSON schema"
        validate(response, get("/domains/$testDomainId/processes/json-schema")).empty

        when: "associating process with a second domain"
        post("/domains/$dsgvoTestDomainId/processes/$processId", [
            subType: "PRO_DataProcessing",
            status: "IN_PROGRESS"
        ], 200)

        and: "fetching process in second domain"
        def processInDsgvo = parseJson(get("/domains/$dsgvoTestDomainId/processes/$processId")) as Map

        then: "it contains basic values"
        processInDsgvo.name == "Marketing"
        processInDsgvo.description == "Catering to the target market"

        and: "values for second domain"
        processInDsgvo.subType == "PRO_DataProcessing"
        processInDsgvo.status == "IN_PROGRESS"

        and: "no values for original domain"
        processInDsgvo.customAspects.general == null

        when: "updating and reloading the process from the viewpoint of the second domain"
        processInDsgvo.description = "New description"
        processInDsgvo.status = "ARCHIVED"
        processInDsgvo.customAspects.process_accessAuthorization = [
            process_accessAuthorization_description: "Uhm..."
        ]
        put("/domains/$dsgvoTestDomainId/processes/$processId", processInDsgvo, [
            'If-Match': getETag(get("/domains/$dsgvoTestDomainId/processes/$processId"))
        ], 200)
        processInDsgvo = parseJson(get("/domains/$dsgvoTestDomainId/processes/$processId"))

        then: "updated values are present"
        processInDsgvo.description == "New description"
        processInDsgvo.status == "ARCHIVED"
        processInDsgvo.customAspects.process_accessAuthorization.process_accessAuthorization_description == "Uhm..."

        and: "values for original domain are still absent"
        processInDsgvo.customAspects.general == null

        when: "fetching the process from the viewpoint of the original domain again"
        def processInTestdomain = parseJson(get("/domains/$testDomainId/processes/$processId"))

        then: "values for original domain are unchanged"
        processInTestdomain.subType == "BusinessProcess"
        processInTestdomain.status == "NEW"
        processInTestdomain.customAspects.general.complexity == "high"

        and: "some basic values have been updated"
        processInTestdomain.name == "Marketing"
        processInTestdomain.description == "New description"

        and: "values for the second domain are absent"
        processInTestdomain.customAspects.process_accessAuthorization == null
    }

    def "get all processes in a domain"() {
        given: "15 processes in the domain"
        (1..15).forEach {
            post("/domains/$testDomainId/processes", [
                name: "process $it",
                owner: [targetUri: "/units/$unitId"],
                subType: "BusinessProcess",
                status: "NEW",
            ])
        }

        expect: "page 1 to be available"
        with(parseJson(get("/domains/$testDomainId/processes?size=10&sortBy=designator"))) {
            totalItemCount == 15
            page == 0
            pageCount == 2
            items*.name == (1..10).collect { "process $it" }
            items*.subType =~ ["BusinessProcess"]
        }

        and: "page 2 to be available"
        with(parseJson(get("/domains/$testDomainId/processes?size=10&page=1&sortBy=designator"))) {
            totalItemCount == 15
            page == 1
            pageCount == 2
            items*.name == (11..15).collect { "process $it" }
            items*.subType =~ ["BusinessProcess"]
        }
    }

    def "missing process is handled"() {
        given: "a non-existing process ID"
        def randomProcessId = randomUUID()

        when: "trying to fetch it in the domain"
        get("/domains/$testDomainId/processes/$randomProcessId", 404)

        then:
        def nfEx = thrown(NotFoundException)
        nfEx.message == "process $randomProcessId not found"
    }

    def "risk values can be updated"() {
        given: "a process with risk values"
        def processId = parseJson(post("/domains/$testDomainId/processes", [
            name: "Risky process",
            owner: [targetUri: "/units/$unitId"],
            subType: "BusinessProcess",
            status: "NEW",
            riskValues: [
                riskyDef: [
                    potentialImpacts: [
                        C: 0
                    ]
                ]
            ]
        ])).resourceId

        when: "updating risk values"
        get("/domains/$testDomainId/processes/$processId").with{getResults ->
            def process = parseJson(getResults)
            process.riskValues.riskyDef.potentialImpacts.C = 1
            put(process._self, process, ["If-Match": getETag(getResults)], 200)
        }

        then: "risk values have been altered"
        with(parseJson(get("/domains/$testDomainId/processes/$processId"))) {
            riskValues.riskyDef.potentialImpacts.C == 1
        }
    }

    def "missing domain is handled"() {
        given: "a process in a domain"
        def processId = parseJson(post("/domains/$testDomainId/processes", [
            name: "Some process",
            owner: [targetUri: "/units/$unitId"],
            subType: "BusinessProcess",
            status: "NEW"
        ])).resourceId
        def randomDomainId = randomUUID()

        when: "trying to fetch the process in a non-existing domain"
        get("/domains/$randomDomainId/processes/$processId", 404)

        then:
        def nfEx = thrown(NotFoundException)
        nfEx.message == "domain $randomDomainId not found"
    }

    def "try to create a process without owner"() {
        given: "a request body without an owner"
        Map request = [
            name: 'New process'
        ]

        when: "a request is made to the server"
        post("/domains/$testDomainId/processes", request, 400)

        then: "the process is not created"
        MethodArgumentNotValidException ex = thrown()

        and: "the reason is given"
        ex.message ==~ /.*An owner must be present.*/
    }

    def "try to put a process without a name"() {
        given: "a saved process"
        def processId = parseJson(post("/domains/$testDomainId/processes", [
            name: "Some process",
            owner: [targetUri: "/units/$unitId"],
            subType: "BusinessProcess",
            status: "NEW"
        ])).resourceId

        Map request = [
            // note that currently the name must not be null but it can be empty ("")
            abbreviation: 'u-2',
            description: 'desc',
            owner:
            [
                targetUri: "http://localhost/units/$unitId",
            ]
        ]

        when: "a request is made to the server"
        Map headers = [
            'If-Match': ETag.from(processId, 1)
        ]
        put("/domains/$testDomainId/processes/$processId", request, headers, 400)

        then: "the process is not updated"
        MethodArgumentNotValidException ex = thrown()

        and: "the reason is given"
        ex.message ==~ /.*A name must be present.*/
    }

    def "overwrite a custom aspect attribute"() {
        given: "a saved process"
        def processId = parseJson(post("/domains/$testDomainId/processes", [
            name: "Some process",
            owner: [targetUri: "/units/$unitId"],
            subType: "BusinessProcess",
            status: "NEW",
            customAspects:
            [
                'general' :
                [
                    complexity: 'low'
                ]
            ]
        ])).resourceId

        when: "a request is made to the server"
        Map request = [
            name: 'Some process',
            owner: [targetUri: "/units/$unitId"],
            subType: "BusinessProcess",
            status: "NEW",
            customAspects:
            [
                'general' :
                [
                    complexity: 'high'
                ]
            ]
        ]
        Map headers = [
            'If-Match': ETag.from(processId, 0)
        ]
        def result = parseJson(put("/domains/$testDomainId/processes/$processId", request, headers))

        then: "the process is found"
        with(result) {
            name == 'Some process'
            subType == "BusinessProcess"
            status == "NEW"
            decisionResults == [:]
            riskValues == [:]
        }

        when:
        def entity = txTemplate.execute {
            processRepository.findById(UUID.fromString(processId)).get().tap {
                // make sure that the proxy is resolved
                customAspects.first()
            }
        }

        then:
        entity.name == 'Some process'
        with(entity.customAspects.first()) {
            type == 'general'
            attributes["complexity"] == 'high'
        }
    }

    @WithUserDetails("user@domain.example")
    def "put a process with link"() {
        given: "a created asset and process"
        def assetId = parseJson(post("/domains/$testDomainId/assets", [
            name: 'New Asset',
            subType: "Information",
            status: "CURRENT",
            owner: [
                targetUri: "http://localhost/units/$unitId"
            ]
        ])).resourceId

        Map createProcessRequest = [
            name: 'New process',
            subType: 'BusinessProcess',
            status: 'NEW',
            owner: [
                targetUri: "http://localhost/units/$unitId"
            ]
        ]

        def createProcessResponse = post("/domains/$testDomainId/processes", createProcessRequest)
        def createProcessResult = new JsonSlurper().parseText(createProcessResponse.andReturn().response.contentAsString)

        Map putProcessRequest = [
            name: 'New Process-2',
            owner:
            [
                targetUri: "http://localhost/units/$unitId"
            ],
            subType: "BusinessProcess",
            status: "NEW",
            links:
            [
                'necessaryData' : [
                    [
                        attributes: [
                            essential: true
                        ],
                        target:
                        [
                            targetUri: "http://localhost/assets/$assetId"
                        ]
                    ]
                ]
            ]
        ]

        when: "a request is made to the server"
        Map headers = [
            'If-Match': ETag.from(createProcessResult.resourceId, 0)
        ]
        def result = parseJson(put("/domains/$testDomainId/processes/${createProcessResult.resourceId}", putProcessRequest, headers))

        then: "the process is found"
        result.name == 'New Process-2'

        and: 'there is one type of links'
        def links = result.links
        links.size() == 1

        and: 'there is one link of the expected type'
        def linksOfExpectedType = links.'necessaryData'
        linksOfExpectedType.size() == 1
    }

    @Use(MapGetProperties)
    def "retrieve CI JSON schema if no ControlImplementationDefinition is set"() {
        when:
        def schema = parseJson(get("/domains/$testDomainId/processes/control-implementations/json-schema"))

        then:
        schema.properties.keySet() ==~ [
            '_requirementImplementations',
            'control',
            'description',
            'implementationStatus',
            'owner',
            'responsible'
        ]
    }
}
