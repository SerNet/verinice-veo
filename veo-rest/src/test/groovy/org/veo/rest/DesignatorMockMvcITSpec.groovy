/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Jonas Jordan.
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

import org.veo.core.VeoMvcSpec
import org.veo.core.entity.Domain
import org.veo.core.entity.Unit
import org.veo.core.entity.definitions.SubTypeDefinition
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.UnitRepositoryImpl

@WithUserDetails("user@domain.example")
class DesignatorMockMvcITSpec extends VeoMvcSpec {

    @Autowired
    private ClientRepositoryImpl clientRepository

    @Autowired
    private UnitRepositoryImpl unitRepository

    private Domain domain
    private Unit unit

    def setup() {
        txTemplate.execute {
            def client = createTestClient()
            newDomain(client) {
                elementTypeDefinitions = [
                    newElementTypeDefinition("asset", it) {
                        subTypes = [
                            Server: newSubTypeDefinition()
                        ]
                    },
                    newElementTypeDefinition("process", it) {
                        subTypes = [
                            Development: newSubTypeDefinition()
                        ]
                    },
                    newElementTypeDefinition("scenario", it) {
                        subTypes = [
                            WorstCase: newSubTypeDefinition()
                        ]
                    },
                ]
            }
            unit = newUnit(client) {
                name = "Test unit"
            }
            clientRepository.save(client)
            unitRepository.save(unit)
            domain = client.domains.first()
        }
    }

    def "designators are generated"() {
        when: "creating an incident"
        String incidentId = parseJson(post("/incidents", [
            name: "incident",
            owner: [
                targetUri: "http://localhost/units/${unit.id.uuidValue()}"
            ]
        ])).resourceId
        def incident = parseJson(get("/incidents/$incidentId"))

        then: "it receives the number one"
        incident.designator == "INC-1"

        when: "creating another incident"
        String newIncidentId = parseJson(post("/incidents", [
            name: "new incident",
            owner: [
                targetUri: "http://localhost/units/${unit.id.uuidValue()}"
            ]
        ])).resourceId
        def newIncident = parseJson(get("/incidents/$newIncidentId"))

        then: "it receives number two"
        newIncident.designator == "INC-2"

        when: "creating a process"
        String processId = parseJson(post("/processes", [
            name: "process",
            owner: [
                targetUri: "http://localhost/units/${unit.id.uuidValue()}"
            ]
        ])).resourceId
        def process = parseJson(get("/processes/$processId"))

        then: "it receives number one"
        process.designator == "PRO-1"
    }

    def "different risks share the same numbering"() {
        given:
        String scenarioId = parseJson(post("/scenarios", [
            name: "scenario",
            owner: [
                targetUri: "http://localhost/units/${unit.id.uuidValue()}"
            ],
            domains: [
                (domain.idAsString): [
                    subType: "WorstCase",
                    status: "NEW",
                ]
            ],
        ])).resourceId

        when: "creating an asset risk"
        String assetId = parseJson(post("/assets", [
            domains: [
                (domain.idAsString): [
                    subType: "Server",
                    status: "NEW",
                ]
            ],
            name: "asset",
            owner: [
                targetUri: "http://localhost/units/${unit.id.uuidValue()}"
            ]
        ])).resourceId
        post("/assets/$assetId/risks", [
            domains: [
                (domain.getIdAsString()): [
                    reference: [ targetUri: "http://localhost/domains/${domain.id.uuidValue()}"]
                ]
            ],
            scenario: [ targetUri: "/scenarios/$scenarioId"]
        ])
        def assetRisk = parseJson(get("/assets/$assetId/risks/$scenarioId"))

        then: "it becomes risk no. 1"
        assetRisk.designator == "RSK-1"

        when: "creating an process risk"
        String processId = parseJson(post("/processes", [
            domains: [
                (domain.id.uuidValue()): [
                    subType: "Development",
                    status: "NEW"
                ]
            ],
            name: "process",
            owner: [
                targetUri: "http://localhost/units/${unit.id.uuidValue()}"
            ]
        ])).resourceId
        post("/processes/$processId/risks", [
            domains: [
                (domain.getIdAsString()) : [
                    reference: [ targetUri: "http://localhost/domains/${domain.id.uuidValue()}"]
                ]
            ],
            scenario: [ targetUri: "http://localhost/scenarios/$scenarioId"]
        ])
        def processRisk = parseJson(get("/processes/$processId/risks/$scenarioId"))

        then: "it becomes risk no. 2"
        processRisk.designator == "RSK-2"
    }

    def "designator can't be changed"() {
        given: "an incident"
        String incidentId = parseJson(post("/incidents", [
            name: "incident",
            owner: [
                targetUri: "http://localhost/units/${unit.id.uuidValue()}"
            ]
        ])).resourceId
        def eTag = parseETag(get("/incidents/$incidentId"))

        when: "trying to update the designator"
        put("/incidents/$incidentId", [
            name: "updated incident",
            designator: "INC-666",
            owner: [
                targetUri: "http://localhost/units/${unit.id.uuidValue()}"
            ]
        ], ["If-Match": eTag])
        def updatedIncident = parseJson(get("/incidents/$incidentId"))

        then: "it remains the same"
        updatedIncident.name == "updated incident"
        updatedIncident.designator == "INC-1"
    }

    def "numbers of deleted objects are not recycled"() {
        when: "creating a document"
        String documentId = parseJson(post("/documents", [
            name: "doc",
            owner: [
                targetUri: "http://localhost/units/${unit.id.uuidValue()}"
            ]
        ])).resourceId
        def document = parseJson(get("/documents/$documentId"))

        then: "it receives the number one"
        document.designator == "DOC-1"

        when: "deleting number one and creating a new document"
        delete("/documents/$documentId")
        String newDocumentId = parseJson(post("/documents", [
            name: "new doc",
            owner: [
                targetUri: "http://localhost/units/${unit.id.uuidValue()}"
            ]
        ])).resourceId
        def newDocument = parseJson(get("/documents/$newDocumentId"))

        then: "it receives number two"
        newDocument.designator == "DOC-2"
    }
}
