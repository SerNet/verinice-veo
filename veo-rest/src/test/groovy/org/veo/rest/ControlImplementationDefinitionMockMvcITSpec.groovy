/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2026  Aziz Khalledi
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
import org.veo.core.entity.Client
import org.veo.core.entity.Domain
import org.veo.core.entity.Unit
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.DomainRepositoryImpl
import org.veo.persistence.access.UnitRepositoryImpl

/**
 * Integration test for control implementation attribute definitions endpoint.
 *
 * Tests the ability to define custom aspects for control implementations
 * on risk-affected element types (Asset, Process, Scope).
 */
@WithUserDetails("content-creator")
class ControlImplementationDefinitionMockMvcITSpec extends VeoMvcSpec {
    @Autowired
    ClientRepositoryImpl clientRepository

    @Autowired
    DomainRepositoryImpl domainRepository

    @Autowired
    UnitRepositoryImpl unitRepository

    Domain dsgvoDomain
    Unit unit
    Client client

    def setup() {
        txTemplate.execute {
            client = createTestClient()
            dsgvoDomain = createTestDomain(client, DSGVO_TEST_DOMAIN_TEMPLATE_ID)
            unit = unitRepository.save(newUnit(client) {
                name = "Test unit"
            })
        }
    }

    def "can define control implementation attributes for assets"() {
        given: "a test domain"
        def domainId = parseJson(post('/content-creation/domains', [
            name: "Test Domain CI",
            authority: "test",
        ])).resourceId

        when: "defining CI custom aspects for assets"
        def ciAttributeDefinition = [
            customAspects: [
                ciDetails: [
                    attributeDefinitions: [
                        implementationNotes: [
                            type: "text"
                        ],
                        priority: [
                            type: "text"
                        ]
                    ]
                ]
            ],
            translations: [
                de: [
                    implementationNotes: "Implementierungsnotizen",
                    priority: "Priorität"
                ]
            ]
        ]

        put("/content-creation/domains/$domainId/element-type-definitions/asset/control-implementation",
                ciAttributeDefinition,
                204)

        then: "the definition is stored"
        def domain = parseJson(get("/domains/$domainId"))
        domain.elementTypeDefinitions.asset.controlImplementationDefinition != null
        with(domain.elementTypeDefinitions.asset.controlImplementationDefinition) {
            it != null
            it.customAspects.ciDetails != null
            it.translations.de.implementationNotes != null
            it.translations.de.priority != null
        }
    }

    def "can define different CI attributes for different element types"() {
        given: "a test domain"
        def domainId = parseJson(post('/content-creation/domains', [
            name: "Test Domain CI Multiple",
            authority: "test2",
        ])).resourceId

        when: "defining different CI custom aspects for asset, process, and scope"
        put("/content-creation/domains/$domainId/element-type-definitions/asset/control-implementation", [
            customAspects: [
                assetCIDetails: [
                    attributeDefinitions: [notes: [type: "text"]]
                ]
            ],
            translations: [
                de: [
                    notes: "Notizen"
                ]
            ]
        ], 204)

        put("/content-creation/domains/$domainId/element-type-definitions/process/control-implementation", [
            customAspects: [
                processCIDetails: [
                    attributeDefinitions: [
                        processNotes: [type: "text"],
                        reviewDate: [type: "text"]
                    ]
                ]
            ],
            translations: [
                de: [
                    processNotes: "Prozessnotizen",
                    reviewDate: "Überprüfungsdatum"
                ]
            ]
        ], 204)

        put("/content-creation/domains/$domainId/element-type-definitions/scope/control-implementation", [
            customAspects: [
                scopeCIDetails: [
                    attributeDefinitions: [scopeNotes: [type: "text"]]
                ]
            ],
            translations: [
                de: [
                    scopeNotes: "Bereichsnotizen"
                ]
            ]
        ], 204)

        then: "each element type has its own CI attributes"
        def domain = parseJson(get("/domains/$domainId"))
        with(domain.elementTypeDefinitions) {
            with(it.asset.controlImplementationDefinition) {
                it.customAspects.assetCIDetails != null
                it.translations.de.notes != null
            }
            with(it.process.controlImplementationDefinition) {
                it.customAspects.processCIDetails != null
                it.translations.de.processNotes != null
            }
            with(it.scope.controlImplementationDefinition) {
                it.customAspects.scopeCIDetails != null
                it.translations.de.scopeNotes != null
            }
        }
    }

    def "cannot define CI attributes for non-risk-affected element types"() {
        given: "a test domain"
        def domainId = parseJson(post('/content-creation/domains', [
            name: "Test Domain CI Invalid",
            authority: "test3",
        ])).resourceId

        when: "trying to define CI attributes for a control (not risk-affected)"
        put("/content-creation/domains/$domainId/element-type-definitions/control/control-implementation", [
            customAspects: [test: [:]]
        ], 400)

        then: "the request is rejected with 400 status"
        def nfEx = thrown(IllegalArgumentException)
        nfEx.message == "Control implementation attributes can only be defined for risk-affected element types (Asset, Process, Scope). Got: CONTROL"
    }

    def "CI attributes are preserved in domain template export/import"() {
        given: "a domain with CI attributes"
        def domainId = parseJson(post('/content-creation/domains', [
            name: "Export Test Domain",
            authority: "export-test",
        ])).resourceId

        put("/content-creation/domains/$domainId/element-type-definitions/asset/control-implementation", [
            customAspects: [
                exportTestCA: [
                    attributeDefinitions: [testField: [type: "text"]]
                ]
            ],
            translations: [
                de: [
                    testField: "Testfeld"
                ]
            ]
        ], 204)

        when: "creating a template from the domain"
        def template = parseJson(post("/content-creation/domains/$domainId/template", [version: "1.0.0"]))

        and: "creating a new domain from the template"
        def newDomainId = createTestDomain(client, UUID.fromString(template.id)).id

        then: "CI attributes are preserved in the imported domain"
        def importedDomain = parseJson(get("/domains/$newDomainId"))
        with(importedDomain.elementTypeDefinitions.asset.controlImplementationDefinition) {
            it != null
            it.customAspects.exportTestCA != null
            it.translations.de.testField != null
        }
    }
}
