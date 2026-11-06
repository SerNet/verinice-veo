/*
 * verinice.veo
 * Copyright (C) 2026  Jochen Kemnade
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
 */
package org.veo.rest

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.test.context.support.WithUserDetails

import org.veo.core.VeoMvcSpec
import org.veo.core.entity.Element
import org.veo.core.entity.TranslatedText
import org.veo.core.entity.condition.ConstantExpression
import org.veo.core.entity.inspection.Severity
import org.veo.core.repository.GenericElementRepository
import org.veo.persistence.access.jpa.InspectionFindingDataRepository

/**
 * Checks that persisted inspection findings are created along with new elements and removed along
 * with deleted elements.
 */
@WithUserDetails("user@domain.example")
class InspectionFindingMvcITSpec extends VeoMvcSpec {

    @Autowired
    InspectionFindingDataRepository inspectionFindingDataRepository

    @Autowired
    GenericElementRepository genericElementRepository

    String domainId
    String unitId

    def setup() {
        domainId = executeInTransaction {
            def client = createTestClient()
            def domain = createTestDomain(client, DSGVO_TEST_DOMAIN_TEMPLATE_ID)
            // applies to every element in the domain
            domain.applyInspection('always-warn', newInspection {
                severity = Severity.WARNING
                condition = new ConstantExpression(true)
                description = TranslatedText.of { 'fix it' }
            })
            domainDataRepository.save(domain).idAsString
        }
        unitId = parseJson(post('/units', [
            name: 'my unit',
            domains: [
                [targetUri: "/domains/$domainId"]
            ]
        ])).resourceId
    }

    def "a new element gets its findings"() {
        when:
        def processId = createProcess()

        then:
        findingCount() == 1
        parseJson(get("/processes/$processId/inspection?domain=$domainId"))
                .any { it.description.en == 'fix it' }
    }

    def "imported elements get their findings"() {
        given:
        def importedUnitId = UUID.randomUUID().toString()
        def request = [
            unit: [
                id: importedUnitId,
                name: 'imported unit',
                domains: [
                    [targetUri: "/domains/$domainId"]
                ]
            ],
            elements: [
                [
                    id: UUID.randomUUID().toString(),
                    name: 'imported process',
                    owner: [targetUri: "/units/$importedUnitId"],
                    type: 'process',
                    domains: [
                        (domainId): [
                            subType: 'PRO_DataProcessing',
                            status: 'NEW'
                        ]
                    ]
                ]
            ],
            domains: [
                parseJson(get("/domains/$domainId"))
            ],
            risks: []
        ]

        when:
        post('/units/import', request)

        then:
        findingCount() == 1
    }

    def "deleting an element deletes its findings"() {
        given:
        def processId = createProcess()

        expect:
        findingCount() == 1

        when:
        delete("/processes/$processId")

        then:
        findingCount() == 0
    }

    def "deleting a unit deletes the findings of its elements"() {
        given:
        createProcess()
        createProcess()

        expect:
        findingCount() == 2

        when:
        delete("/units/$unitId")

        then:
        findingCount() == 0
    }

    def "deleting several elements at once deletes their findings"() {
        given:
        createProcess()
        createProcess()

        expect:
        findingCount() == 2

        when:
        executeInTransaction {
            genericElementRepository.deleteAll(processDataRepository.findAll() as List<Element>)
        }

        then:
        findingCount() == 0
    }

    private String createProcess() {
        parseJson(post("/domains/$domainId/processes", [
            name: 'my process',
            owner: [targetUri: "/units/$unitId"],
            subType: 'PRO_DataProcessing',
            status: 'NEW'
        ])).resourceId
    }

    private long findingCount() {
        inspectionFindingDataRepository.count()
    }
}
