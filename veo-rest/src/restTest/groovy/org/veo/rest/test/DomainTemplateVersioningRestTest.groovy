/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2024  Jonas Jordan
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

import static org.veo.rest.test.UserType.ADMIN
import static org.veo.rest.test.UserType.CONTENT_CREATOR

class DomainTemplateVersioningRestTest extends DomainRestTest {

    private String domainName
    private String domainId
    private String templateId_1_0_0

    def setup() {
        domainName = "domain template versioning rest test ${UUID.randomUUID()}"
        domainId = post("/content-creation/domains", [
            name: domainName,
            authority: "me",
        ], 201, CONTENT_CREATOR).body.resourceId
        putPersonDefinition(domainId)

        templateId_1_0_0 = post("/content-creation/domains/$domainId/template", [version: "1.0.0"], 201, CONTENT_CREATOR).body.id
    }

    def "template versioning is validated"() {
        when: "creating a second template"
        post("/content-creation/domains/$domainId/template", [version: "1.1.0"], 201, CONTENT_CREATOR).body.id

        then: "both templates exist"
        get("/domain-templates").body.findAll { it.name == domainName }*.templateVersion ==~ ["1.0.0", "1.1.0"]

        expect: "that new version numbers must be valid semvers without any labels"
        post("/content-creation/domains/$domainId/template", [version: "1.2"], 400, CONTENT_CREATOR)
        .body.version == 'must match "[0-9]+\\.[0-9]+\\.[0-9]+"'
        post("/content-creation/domains/$domainId/template", [version: "1.2.0-prerelease3"], 400, CONTENT_CREATOR)
        .body.version == 'must match "[0-9]+\\.[0-9]+\\.[0-9]+"'

        and: "that new version numbers must be higher"
        post("/content-creation/domains/$domainId/template", [version: "0.9.0"], 422, CONTENT_CREATOR)
        .body.message == "Unexpected version - expected next patch (1.1.1), minor (1.2.0) or major (2.0.0)."
        post("/content-creation/domains/$domainId/template", [version: "1.1.0"], 409, CONTENT_CREATOR)
        .body.message == "Domain template $domainName 1.1.0 already exists"

        and: "that versions cannot be skipped"
        ["1.1.2", "1.3.0", "3.0.0"].each {
            assert post("/content-creation/domains/$domainId/template", [version: it], 422, CONTENT_CREATOR)
            .body.message == "Unexpected version - expected next patch (1.1.1), minor (1.2.0) or major (2.0.0)."
        }
    }

    def "hotfix can be created from old minor version"() {
        when: "creating a new minor version template"
        post("/content-creation/domains/$domainId/template", [version: "1.1.0"], 201, CONTENT_CREATOR).body.id

        and: "creating a hotfix domain from the original template version"
        post("/domain-templates/$templateId_1_0_0/createdomains", [:], 204, ADMIN)
        def oldMinorDomainId = get("/domains").body.find { it.name == domainName && it.templateVersion == "1.0.0" }.id

        then: "the outdated domain cannot be used to create a new minor or major version"
        post("/content-creation/domains/$oldMinorDomainId/template", [version: "2.0.0"], 422, CONTENT_CREATOR)
        .body.message == "Given domain is based on version 1.0.0, but a new minor or major version can only be created from the latest template 1.1.0."
        post("/content-creation/domains/$oldMinorDomainId/template", [version: "1.1.0"], 409, CONTENT_CREATOR)
        .body.message == "Domain template $domainName 1.1.0 already exists"

        when: "creating a patch version from the old minor"
        post("/content-creation/domains/$oldMinorDomainId/template", [version: "1.0.1"], 201, CONTENT_CREATOR).body.id

        then: "all three templates exist"
        get("/domain-templates").body.findAll { it.name == domainName }*.templateVersion ==~ ["1.0.0", "1.0.1", "1.1.0"]

        when: "creating yet another domain from the original version"
        post("/domain-templates/$templateId_1_0_0/createdomains", [:], 204, ADMIN)
        def anotherOldMinorDomainId = get("/domains").body.find { it.name == domainName && it.templateVersion == "1.0.0" }.id

        then: "that outdated domain cannot be used to create yet another patch"
        post("/content-creation/domains/$anotherOldMinorDomainId/template", [version: "1.0.1"], 409, CONTENT_CREATOR)
        .body.message == "Domain template $domainName 1.0.1 already exists"
        post("/content-creation/domains/$anotherOldMinorDomainId/template", [version: "1.0.2"], 422, CONTENT_CREATOR)
        .body.message == "Unexpected version - expected next patch (1.0.1), minor (1.1.0) or major (2.0.0)."
    }

    def "hotfix can be created from old major version"() {
        when: "creating a new major version template"
        post("/content-creation/domains/$domainId/template", [version: "2.0.0"], 201, CONTENT_CREATOR).body.id

        and: "creating a hotfix domain from the original template version"
        post("/domain-templates/$templateId_1_0_0/createdomains", [:], 204, ADMIN)
        def oldMajorDomainId = get("/domains").body.find { it.name == domainName && it.templateVersion == "1.0.0" }.id

        then: "the outdated domain cannot be used to create a new minor or major version"
        post("/content-creation/domains/$oldMajorDomainId/template", [version: "1.1.0"], 422, CONTENT_CREATOR)
        .body.message == "Given domain is based on version 1.0.0, but a new minor or major version can only be created from the latest template 2.0.0."
        post("/content-creation/domains/$oldMajorDomainId/template", [version: "2.0.0"], 409, CONTENT_CREATOR)
        .body.message == "Domain template $domainName 2.0.0 already exists"

        when: "creating a patch version from the old major"
        post("/content-creation/domains/$oldMajorDomainId/template", [version: "1.0.1"], 201, CONTENT_CREATOR).body.id

        then: "all three templates exist"
        get("/domain-templates").body.findAll { it.name == domainName }*.templateVersion ==~ ["1.0.0", "1.0.1", "2.0.0"]
    }

    def "breaking changes can only be released as a major"() {
        when: "breaking things"
        get("/domains/$domainId").body.elementTypeDefinitions.person.with{
            customAspects.sight.attributeDefinitions.remove("needsGlasses")
            customAspects.sight.attributeDefinitions.needsReadingGlasses = [type: "boolean"]
            customAspects.sight.attributeDefinitions.needsDistanceSpecs = [type: "boolean"]
            customAspects.sight.attributeDefinitions.remove("nightBlind")
            customAspects.sight.attributeDefinitions.nightVision = [type: "boolean"]

            put("/content-creation/domains/$owner.domainId/element-type-definitions/person", it, null, 204, CONTENT_CREATOR)
        }

        then: "a template cannot be created without migration steps"
        ["1.0.1", "1.1.0", "2.0.0"].each{
            with(post("/content-creation/domains/$domainId/template", [version: it], 422, CONTENT_CREATOR)
            .body.message) {
                it.startsWith("Migration definition not suited to update from old domain template 1.0.0: Missing migration steps:")
                it.contains("Removed attribute 'needsGlasses' of custom aspect 'sight' for type person")
                it.contains("Removed attribute 'nightBlind' of custom aspect 'sight' for type person")
            }
        }

        when: "adding migration steps"
        put("/content-creation/domains/$domainId/migrations", [
            [
                id: "split-needsGlasses",
                description: [en: "Things have changed."],
                oldDefinitions: [
                    [
                        type: "customAspectAttribute",
                        elementType: "person",
                        customAspect: "sight",
                        attribute: "needsGlasses"
                    ]
                ],
                newDefinitions: [
                    [
                        type: "customAspectAttribute",
                        elementType: "person",
                        customAspect: "sight",
                        attribute: "needsReadingGlasses",
                        migrationExpression: [
                            type: "customAspectAttributeValue",
                            customAspect: "sight",
                            attribute: "needsGlasses",
                        ]
                    ],
                    [
                        type: "customAspectAttribute",
                        elementType: "person",
                        customAspect: "sight",
                        attribute: "needsDistanceSpecs",
                        migrationExpression: [
                            type: "customAspectAttributeValue",
                            customAspect: "sight",
                            attribute: "needsGlasses",
                        ]
                    ]
                ]
            ],
            [
                id: "negate-night-blindness",
                description: [en: "Things have changed."],
                oldDefinitions: [
                    [
                        type: "customAspectAttribute",
                        elementType: "person",
                        customAspect: "sight",
                        attribute: "nightBlind"
                    ]
                ],
                newDefinitions: [
                    [
                        type: "customAspectAttribute",
                        elementType: "person",
                        customAspect: "sight",
                        attribute: "nightVision",
                        migrationExpression: [
                            type: "equals",
                            left: [
                                type: "customAspectAttributeValue",
                                customAspect: "sight",
                                attribute: "nightBlind",
                            ],
                            right: [
                                type: "constant",
                                value: false,
                            ]
                        ]
                    ],
                ]
            ]
        ], null, 200)

        then: "it still cannot be released as a patch or minor"
        post("/content-creation/domains/$domainId/template", [version: "1.0.1"], 422, CONTENT_CREATOR)
        .body.message == "Domain contains breaking changes and must be released as a major (2.0.0) update."
        post("/content-creation/domains/$domainId/template", [version: "1.1.0"], 422, CONTENT_CREATOR)
        .body.message == "Domain contains breaking changes and must be released as a major (2.0.0) update."

        and: "it can be released as a major"
        post("/content-creation/domains/$domainId/template", [version: "2.0.0"], 201, CONTENT_CREATOR).body.id

        when: "releasing improved migration steps as a patch"
        get("/content-creation/domains/$domainId/migrations", 200, CONTENT_CREATOR).body.with{ migrations ->
            migrations[0].description.en = "Instead of just telling us whether you need glasses or not, we need to know which kinds of glasses you need."
            put("/content-creation/domains/$owner.domainId/migrations", migrations, null, 200)
        }
        post("/content-creation/domains/$domainId/template", [version: "2.0.1"], 201, CONTENT_CREATOR).body.id

        and: "making more breaking changes"
        get("/domains/$domainId").body.elementTypeDefinitions.person.with{
            customAspects.eyeSight = customAspects.sight
            customAspects.remove("sight")
            put("/content-creation/domains/$owner.domainId/element-type-definitions/person", it, null, 204, CONTENT_CREATOR)
        }

        then: "the migration steps are no longer valid"
        post("/content-creation/domains/$domainId/template", [version: "3.0.0"], 422, CONTENT_CREATOR)
        .body.message == "Migration definition not suited to update from old domain template 2.0.1: Invalid newDefinition 'split-needsGlasses'. No customAspect 'sight' for element type person."

        when: "creating a new migration step"
        put("/content-creation/domains/$domainId/migrations", [
            [
                id: "rename-sight",
                description: [en: "We have renamed a custom aspect key."],
                oldDefinitions: [
                    [
                        type: "customAspectAttribute",
                        elementType: "person",
                        customAspect: "sight",
                        attribute: "needsDistanceSpecs",
                    ],
                    [
                        type: "customAspectAttribute",
                        elementType: "person",
                        customAspect: "sight",
                        attribute: "needsReadingGlasses",
                    ],
                    [
                        type: "customAspectAttribute",
                        elementType: "person",
                        customAspect: "sight",
                        attribute: "nightVision",
                    ]
                ],
                newDefinitions: [
                    [
                        type: "customAspectAttribute",
                        elementType: "person",
                        customAspect: "eyeSight",
                        attribute: "needsReadingGlasses",
                        migrationExpression: [
                            type: "customAspectAttributeValue",
                            customAspect: "sight",
                            attribute: "needsReadingGlasses",
                        ]
                    ],
                    [
                        type: "customAspectAttribute",
                        elementType: "person",
                        customAspect: "eyeSight",
                        attribute: "needsDistanceSpecs",
                        migrationExpression: [
                            type: "customAspectAttributeValue",
                            customAspect: "sight",
                            attribute: "needsDistanceSpecs",
                        ]
                    ],
                    [
                        type: "customAspectAttribute",
                        elementType: "person",
                        customAspect: "eyeSight",
                        attribute: "nightVision",
                        migrationExpression: [
                            type: "customAspectAttributeValue",
                            customAspect: "sight",
                            attribute: "nightVision",
                        ]
                    ]
                ]
            ]
        ], null, 200)

        then: "a new major can be created"
        post("/content-creation/domains/$domainId/template", [version: "3.0.0"], 201, CONTENT_CREATOR)
        get("/domain-templates").body.findAll { it.name == domainName }*.templateVersion ==~ [
            "1.0.0",
            "2.0.0",
            "2.0.1",
            "3.0.0"
        ]
    }
}
