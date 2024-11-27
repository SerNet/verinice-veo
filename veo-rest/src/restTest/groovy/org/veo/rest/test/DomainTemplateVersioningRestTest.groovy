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
    private String domainId_original
    private String domainId_1_0_0
    private String templateId_1_0_0

    def setup() {
        domainName = "domain template versioning rest test ${UUID.randomUUID()}"
        domainId_original = post("/content-creation/domains", [
            name: domainName,
            authority: "me",
        ], 201, CONTENT_CREATOR).body.resourceId
        putPersonDefinition(domainId_original)

        templateId_1_0_0 = post("/content-creation/domains/$domainId_original/template", [version: "1.0.0"], 201, CONTENT_CREATOR).body.id
        // TODO #3301 createdomains becomes unnecessary once the template gets associated with the domain it was created from
        post("/domain-templates/$templateId_1_0_0/createdomains", null, 204, ADMIN)
        domainId_1_0_0 = get("/domains").body.find { it.name == domainName && it.id != domainId_original }.id
    }

    def "template versioning is validated"() {
        when: "creating a second template"
        def templateId_1_1_0 = post("/content-creation/domains/$domainId_1_0_0/template", [version: "1.1.0"], 201, CONTENT_CREATOR).body.id
        // TODO #3301 createdomains becomes unnecessary once the template gets associated with the domain it was created from
        post("/domain-templates/$templateId_1_1_0/createdomains", null, 204, ADMIN)
        def domainId_1_1_0 = get("/domains").body.find { it.name == domainName && it.templateVersion == "1.1.0" && it.id != domainId_1_0_0 }.id

        then: "both templates exist"
        get("/domain-templates").body.findAll { it.name == domainName }*.templateVersion ==~ ["1.0.0", "1.1.0"]

        expect: "that new version numbers must be valid semvers without any labels"
        post("/content-creation/domains/$domainId_1_1_0/template", [version: "1.2"], 400, CONTENT_CREATOR)
        .body.version == 'must match "[0-9]+\\.[0-9]+\\.[0-9]+"'
        post("/content-creation/domains/$domainId_1_1_0/template", [version: "1.2.0-prerelease3"], 400, CONTENT_CREATOR)
        .body.version == 'must match "[0-9]+\\.[0-9]+\\.[0-9]+"'

        and: "that new version numbers must be higher"
        post("/content-creation/domains/$domainId_1_1_0/template", [version: "0.9.0"], 422, CONTENT_CREATOR)
        .body.message == "Unexpected version - expected next patch (1.1.1), minor (1.2.0) or major (2.0.0)."
        post("/content-creation/domains/$domainId_1_1_0/template", [version: "1.1.0"], 409, CONTENT_CREATOR)
        .body.message == "Domain template $domainName 1.1.0 already exists"

        and: "that versions cannot be skipped"
        ["1.1.2", "1.3.0", "3.0.0"].each {
            assert post("/content-creation/domains/$domainId_1_1_0/template", [version: it], 422, CONTENT_CREATOR)
            .body.message == "Unexpected version - expected next patch (1.1.1), minor (1.2.0) or major (2.0.0)."
        }
    }

    def "hotfix can be created from old minor version"() {
        when: "creating a new minor version template"
        post("/content-creation/domains/$domainId_1_0_0/template", [version: "1.1.0"], 201, CONTENT_CREATOR).body.id

        // TODO #3301 create a new domain from the old template and use that as our old minor version here

        then: "the outdated domain cannot be used to create a new minor or major version"
        post("/content-creation/domains/$domainId_1_0_0/template", [version: "2.0.0"], 422, CONTENT_CREATOR)
        .body.message == "Given domain is based on version 1.0.0, but a new minor or major version can only be created from the latest template 1.1.0."
        post("/content-creation/domains/$domainId_1_0_0/template", [version: "1.1.0"], 409, CONTENT_CREATOR)
        .body.message == "Domain template $domainName 1.1.0 already exists"

        when: "creating a patch version from the old minor"
        post("/content-creation/domains/$domainId_1_0_0/template", [version: "1.0.1"], 201, CONTENT_CREATOR).body.id

        then: "all three templates exist"
        get("/domain-templates").body.findAll { it.name == domainName }*.templateVersion ==~ ["1.0.0", "1.0.1", "1.1.0"]

        expect: "that the old minor cannot be used to create yet another patch"
        // TODO #3301 create a new domain from the old template and use that as our old minor version here
        post("/content-creation/domains/$domainId_1_0_0/template", [version: "1.0.1"], 409, CONTENT_CREATOR)
        .body.message == "Domain template $domainName 1.0.1 already exists"
        post("/content-creation/domains/$domainId_1_0_0/template", [version: "1.0.2"], 422, CONTENT_CREATOR)
        .body.message == "Unexpected version - expected next patch (1.0.1), minor (1.1.0) or major (2.0.0)."
    }

    def "hotfix can be created from old major version"() {
        when: "creating a new major version template"
        post("/content-creation/domains/$domainId_1_0_0/template", [version: "2.0.0"], 201, CONTENT_CREATOR).body.id

        // TODO #3301 create a new domain from the old template and use that as our old minor version here

        then: "the outdated domain cannot be used to create a new minor or major version"
        post("/content-creation/domains/$domainId_1_0_0/template", [version: "1.1.0"], 422, CONTENT_CREATOR)
        .body.message == "Given domain is based on version 1.0.0, but a new minor or major version can only be created from the latest template 2.0.0."
        post("/content-creation/domains/$domainId_1_0_0/template", [version: "2.0.0"], 409, CONTENT_CREATOR)
        .body.message == "Domain template $domainName 2.0.0 already exists"

        when: "creating a patch version from the old major"
        post("/content-creation/domains/$domainId_1_0_0/template", [version: "1.0.1"], 201, CONTENT_CREATOR).body.id

        then: "all three templates exist"
        get("/domain-templates").body.findAll { it.name == domainName }*.templateVersion ==~ ["1.0.0", "1.0.1", "2.0.0"]
    }

    def "breaking changes can only be released as a major"() {
        when: "breaking things"
        get("/domains/$domainId_1_0_0").body.elementTypeDefinitions.person.with{
            customAspects.sight.attributeDefinitions.remove("needsGlasses")
            customAspects.sight.attributeDefinitions.needsReadingGlasses = [type: "boolean"]
            customAspects.sight.attributeDefinitions.needsDistanceSpecs = [type: "boolean"]
            customAspects.sight.attributeDefinitions.remove("nightBlind")
            customAspects.sight.attributeDefinitions.nightVision = [type: "boolean"]

            put("/content-creation/domains/$owner.domainId_1_0_0/element-type-definitions/person", it, null, 204, CONTENT_CREATOR)
        }

        then: "a template cannot be created without migration steps"
        ["1.0.1", "1.1.0", "2.0.0"].each{
            with(post("/content-creation/domains/$domainId_1_0_0/template", [version: it], 422, CONTENT_CREATOR)
            .body.message) {
                it.startsWith("Migration definition not suited to update from old domain template 1.0.0: Missing migration steps:")
                it.contains("Removed attribute 'needsGlasses' of custom aspect 'sight' for type person")
                it.contains("Removed attribute 'nightBlind' of custom aspect 'sight' for type person")
            }
        }

        when: "adding migration steps"
        put("/content-creation/domains/$domainId_1_0_0/migrations", [
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
                        attribute: "needsReadingGlasses"
                    ],
                    [
                        type: "customAspectAttribute",
                        elementType: "person",
                        customAspect: "sight",
                        attribute: "needsDistanceSpecs"
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
        post("/content-creation/domains/$domainId_1_0_0/template", [version: "1.0.1"], 422, CONTENT_CREATOR)
        .body.message == "Domain contains breaking changes and must be released as a major (2.0.0) update."
        post("/content-creation/domains/$domainId_1_0_0/template", [version: "1.1.0"], 422, CONTENT_CREATOR)
        .body.message == "Domain contains breaking changes and must be released as a major (2.0.0) update."

        and: "it can be released as a major"
        def templateId_2_0_0 = post("/content-creation/domains/$domainId_1_0_0/template", [version: "2.0.0"], 201, CONTENT_CREATOR).body.id

        when: "releasing improved migration steps as a patch"
        // TODO #3301 createdomains becomes unnecessary once the template gets associated with the domain it was created from
        post("/domain-templates/$templateId_2_0_0/createdomains", null, 204, ADMIN)
        def domainId_2_0_0 = domains.find { it.name == domainName && it.templateVersion == "2.0.0" && it.id != domainId_1_0_0 }.id
        get("/content-creation/domains/$domainId_2_0_0/migrations", 200, CONTENT_CREATOR).body.with{ migrations ->
            migrations[0].description.en = "Instead of just telling us whether you need glasses or not, we need to know which kinds of glasses you need."
            put("/content-creation/domains/$domainId_2_0_0/migrations", migrations, null, 200)
        }
        def templateId_2_0_1 = post("/content-creation/domains/$domainId_2_0_0/template", [version: "2.0.1"], 201, CONTENT_CREATOR).body.id

        and: "making more breaking changes"
        // TODO #3301 createdomains becomes unnecessary once the template gets associated with the domain it was created from
        post("/domain-templates/$templateId_2_0_1/createdomains", null, 204, ADMIN)
        def domainId_2_0_1 = domains.find { it.name == domainName && it.templateVersion == "2.0.1" && it.id != domainId_2_0_0 }.id
        get("/domains/$domainId_2_0_1").body.elementTypeDefinitions.person.with{
            customAspects.eyeSight = customAspects.sight
            customAspects.remove("sight")
            put("/content-creation/domains/$domainId_2_0_1/element-type-definitions/person", it, null, 204, CONTENT_CREATOR)
        }

        then: "the migration steps are no longer valid"
        post("/content-creation/domains/$domainId_2_0_1/template", [version: "3.0.0"], 422, CONTENT_CREATOR)
        .body.message == "Migration definition not suited to update from old domain template 2.0.1: Invalid definition 'split-needsGlasses'. No customAspect 'sight' for element type person in newDefinitions."

        when: "creating a new migration step"
        put("/content-creation/domains/$domainId_2_0_1/migrations", [
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
                        attribute: "needsReadingGlasses"
                    ],
                    [
                        type: "customAspectAttribute",
                        elementType: "person",
                        customAspect: "eyeSight",
                        attribute: "needsDistanceSpecs"
                    ],
                    [
                        type: "customAspectAttribute",
                        elementType: "person",
                        customAspect: "eyeSight",
                        attribute: "nightVision",
                    ]
                ]
            ]
        ], null, 200)

        then: "a new major can be created"
        post("/content-creation/domains/$domainId_2_0_1/template", [version: "3.0.0"], 201, CONTENT_CREATOR)
        get("/domain-templates").body.findAll { it.name == domainName }*.templateVersion ==~ [
            "1.0.0",
            "2.0.0",
            "2.0.1",
            "3.0.0"
        ]
    }

    // TODO #3301 this test becomes obsolete once the new template becomes associated with the domain it was created from
    def "new template version cannot be created from domain that was not created from a template"() {
        expect: "that the original domain that does not refer to the template cannot be used to create another template"
        post("/content-creation/domains/$domainId_original/template", [version: "1.1.0"], 422, CONTENT_CREATOR)
        .body.message == "Domain is not based on a template, but templates already exist."
    }
}
