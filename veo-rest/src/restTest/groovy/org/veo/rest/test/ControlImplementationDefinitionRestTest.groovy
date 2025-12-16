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
package org.veo.rest.test

import static org.veo.rest.test.UserType.CONTENT_CREATOR

class ControlImplementationDefinitionRestTest extends VeoRestTest {

    def "can define control implementation attributes for assets"() {
        given: "a test domain"
        def domainId = post('/content-creation/domains', [
            name: "Test Domain CI ${UUID.randomUUID()}",
            authority: "test-${UUID.randomUUID()}",
        ], 201, CONTENT_CREATOR).body.resourceId

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
                        ],
                        reviewDate: [
                            type: "text"
                        ]
                    ]
                ]
            ]
        ]

        put("/content-creation/domains/$domainId/element-type-definitions/asset/control-implementation",
                ciAttributeDefinition,
                null,
                204,
                CONTENT_CREATOR)

        then: "the definition is stored and retrievable"
        def domain = get("/domains/$domainId").body
        with(domain.elementTypeDefinitions.asset.controlImplementationDefinition) {
            it != null
            with(it.customAspects.ciDetails) {
                it != null
                it.attributeDefinitions.implementationNotes != null
                it.attributeDefinitions.priority != null
                it.attributeDefinitions.reviewDate != null
            }
        }
    }

    def "can define different CI attributes for different element types"() {
        given: "a test domain"
        def domainId = post('/content-creation/domains', [
            name: "Test Domain CI Multiple ${UUID.randomUUID()}",
            authority: "test2-${UUID.randomUUID()}",
        ], 201, CONTENT_CREATOR).body.resourceId

        when: "defining different CI custom aspects for asset, process, and scope"
        put("/content-creation/domains/$domainId/element-type-definitions/asset/control-implementation", [
            customAspects: [
                assetCIDetails: [
                    attributeDefinitions: [
                        assetNotes: [type: "text"],
                        assetOwner: [type: "text"]
                    ]
                ]
            ]
        ], null, 204, CONTENT_CREATOR)

        put("/content-creation/domains/$domainId/element-type-definitions/process/control-implementation", [
            customAspects: [
                processCIDetails: [
                    attributeDefinitions: [
                        processNotes: [type: "text"],
                        reviewDate: [type: "text"],
                        frequency: [type: "text"]
                    ]
                ]
            ]
        ], null, 204, CONTENT_CREATOR)

        put("/content-creation/domains/$domainId/element-type-definitions/scope/control-implementation", [
            customAspects: [
                scopeCIDetails: [
                    attributeDefinitions: [
                        scopeNotes: [type: "text"],
                        coverage: [type: "text"]
                    ]
                ]
            ]
        ], null, 204, CONTENT_CREATOR)

        then: "each element type has its own CI attributes"
        def domain = get("/domains/$domainId").body

        and: "asset has correct CI attributes"
        with(domain.elementTypeDefinitions.asset.controlImplementationDefinition.customAspects.assetCIDetails) {
            it != null
            it.attributeDefinitions.assetNotes != null
            it.attributeDefinitions.assetOwner != null
        }

        and: "process has correct CI attributes"
        with(domain.elementTypeDefinitions.process.controlImplementationDefinition.customAspects.processCIDetails) {
            it != null
            it.attributeDefinitions.processNotes != null
            it.attributeDefinitions.reviewDate != null
            it.attributeDefinitions.frequency != null
        }

        and: "scope has correct CI attributes"
        with(domain.elementTypeDefinitions.scope.controlImplementationDefinition.customAspects.scopeCIDetails) {
            it != null
            it.attributeDefinitions.scopeNotes != null
            it.attributeDefinitions.coverage != null
        }
    }

    def "cannot define CI attributes for non-risk-affected element types"() {
        given: "a test domain"
        def domainId = post('/content-creation/domains', [
            name: "Test Domain CI Invalid ${UUID.randomUUID()}",
            authority: "test3-${UUID.randomUUID()}",
        ], 201, CONTENT_CREATOR).body.resourceId

        when: "trying to define CI attributes for a control (not risk-affected)"
        put("/content-creation/domains/$domainId/element-type-definitions/control/control-implementation", [
            customAspects: [
                invalidCA: [
                    attributeDefinitions: [testField: [type: "text"]]
                ]
            ]
        ], null, 400, CONTENT_CREATOR)

        then: "the request is rejected"
        notThrown(Exception)
    }

    def "CI attributes are preserved in domain template export and import"() {
        given: "a domain with CI attributes"
        def domainName = "Export Test Domain ${UUID.randomUUID()}"
        def domainId = post('/content-creation/domains', [
            name: domainName,
            authority: "export-test-${UUID.randomUUID()}",
        ], 201, CONTENT_CREATOR).body.resourceId

        and: "CI attributes are defined for asset"
        put("/content-creation/domains/$domainId/element-type-definitions/asset/control-implementation", [
            customAspects: [
                exportTestCA: [
                    attributeDefinitions: [
                        testField: [type: "text"],
                        anotherField: [type: "text"]
                    ]
                ]
            ]
        ], null, 204, CONTENT_CREATOR)

        and: "CI attributes are defined for process"
        put("/content-creation/domains/$domainId/element-type-definitions/process/control-implementation", [
            customAspects: [
                processExportCA: [
                    attributeDefinitions: [
                        processField: [type: "text"]
                    ]
                ]
            ]
        ], null, 204, CONTENT_CREATOR)

        when: "creating a template from the domain"
        def templateVersion = "1.0.${System.currentTimeMillis()}"
        def templateId = post("/content-creation/domains/$domainId/template", [
            version: templateVersion
        ], 201, CONTENT_CREATOR).body.id

        and: "creating domains from the template in all clients"
        post("/domain-templates/$templateId/createdomains?restrictToClientsWithExistingDomain=false", [:], 204, UserType.ADMIN)

        then: "wait for domain creation to complete"
        defaultPolling.within(10) {
            def domains = getDomains()
            assert domains.any { it.name == domainName && it.templateVersion == templateVersion }
        }

        and: "CI attributes are preserved in the imported domain"
        def domains = getDomains()
        def importedDomain = domains.find { it.name == domainName && it.templateVersion == templateVersion }
        assert importedDomain != null

        def fullDomain = get("/domains/${importedDomain.id}").body

        and: "asset CI attributes are preserved"
        with(fullDomain.elementTypeDefinitions.asset.controlImplementationDefinition) {
            it != null
            with(it.customAspects.exportTestCA) {
                it != null
                it.attributeDefinitions.testField != null
                it.attributeDefinitions.anotherField != null
            }
        }

        and: "process CI attributes are preserved"
        with(fullDomain.elementTypeDefinitions.process.controlImplementationDefinition) {
            it != null
            with(it.customAspects.processExportCA) {
                it != null
                it.attributeDefinitions.processField != null
            }
        }
    }

    def "can update CI attributes multiple times"() {
        given: "a test domain"
        def domainId = post('/content-creation/domains', [
            name: "Test Domain CI Update ${UUID.randomUUID()}",
            authority: "update-test-${UUID.randomUUID()}",
        ], 201, CONTENT_CREATOR).body.resourceId

        and: "initial CI attributes"
        put("/content-creation/domains/$domainId/element-type-definitions/asset/control-implementation", [
            customAspects: [
                initialCA: [
                    attributeDefinitions: [
                        field1: [type: "text"]
                    ]
                ]
            ]
        ], null, 204, CONTENT_CREATOR)

        when: "updating CI attributes with new custom aspects"
        put("/content-creation/domains/$domainId/element-type-definitions/asset/control-implementation", [
            customAspects: [
                initialCA: [
                    attributeDefinitions: [
                        field1: [type: "text"],
                        field2: [type: "text"]
                    ]
                ],
                newCA: [
                    attributeDefinitions: [
                        newField: [type: "text"]
                    ]
                ]
            ]
        ], null, 204, CONTENT_CREATOR)

        then: "both custom aspects are present"
        def domain = get("/domains/$domainId").body
        with(domain.elementTypeDefinitions.asset.controlImplementationDefinition.customAspects) {
            with(it.initialCA) {
                it != null
                it.attributeDefinitions.field1 != null
                it.attributeDefinitions.field2 != null
            }
            with(it.newCA) {
                it != null
                it.attributeDefinitions.newField != null
            }
        }
    }
}
