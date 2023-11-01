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
package org.veo.rest.test

import org.veo.core.entity.EntityType

class ControlImplementationRestTest extends VeoRestTest {
    private String domainId
    private String unitId
    private String rootControl1Id
    private String rootControl2Id
    private String subControl1Id
    private String subControl2Id
    private String subControl3Id
    private String person1Id
    private String person2Id

    def setup() {
        domainId = post("/content-creation/domains", [
            name: "CI/RI test domain ${UUID.randomUUID()}",
            authority: "JJ",
        ], 201, UserType.CONTENT_CREATOR).body.resourceId
        unitId = postNewUnit().resourceId

        subControl1Id = post("/controls", [
            name: "sub control 1",
            owner: [targetUri: "http://localhost/units/$unitId"]
        ]).body.resourceId
        subControl2Id = post("/controls", [
            name: "sub control 2",
            owner: [targetUri: "http://localhost/units/$unitId"]
        ]).body.resourceId
        rootControl1Id = post("/controls", [
            name: "root control 1",
            owner: [targetUri: "http://localhost/units/$unitId"],
            parts: [
                [targetUri: "http://localhost/controls/$subControl1Id"],
                [targetUri: "http://localhost/controls/$subControl2Id"],
            ],
        ]).body.resourceId
        subControl3Id = post("/controls", [
            name: "sub control 3",
            owner: [targetUri: "http://localhost/units/$unitId"],
        ]).body.resourceId
        rootControl2Id = post("/controls", [
            name: "root control 2",
            owner: [targetUri: "http://localhost/units/$unitId"],
            parts: [
                [targetUri: "http://localhost/controls/$subControl3Id"]
            ],
        ]).body.resourceId

        person1Id = post("/persons", [
            name: "person 1",
            owner: [targetUri: "http://localhost/units/$unitId"],
        ]).body.resourceId
        person2Id = post("/persons", [
            name: "person 2",
            owner: [targetUri: "http://localhost/units/$unitId"],
        ]).body.resourceId
    }

    def "CRUD CIs & RIs for #elementType.singularTerm"() {
        when: "creating and fetching an element with two CIs"
        def elementId = post("/$elementType.pluralTerm", [
            name: "lame",
            owner: [targetUri: "http://localhost/units/$unitId"],
            controlImplementations: [
                [
                    control: [targetUri: "/controls/$rootControl1Id"],
                    description: "I have my reasons",
                ],
                [
                    control: [targetUri: "/controls/$rootControl2Id"],
                    responsible: [targetUri: "/persons/$person1Id"],
                ],
            ]
        ]).body.resourceId
        def retrievedElement = get("/$elementType.pluralTerm/$elementId").body

        then: "CIs and RIs for both controls and all of their parts are present"
        retrievedElement.controlImplementations.size() == 2
        with(retrievedElement.controlImplementations.find { it.control.displayName.endsWith("root control 1") }) {
            implementationStatus == "UNKNOWN"
            description == "I have my reasons"
            with(owner.get(_requirementImplementations).body) {
                totalItemCount == 3
                with(items.find { it.control.displayName.endsWith("root control 1") }) {
                    status == "UNKNOWN"
                }
                with(items.find { it.control.displayName.endsWith("sub control 1") }) {
                    status == "UNKNOWN"
                }
                with(items.find { it.control.displayName.endsWith("sub control 2") }) {
                    status == "UNKNOWN"
                }
            }
        }
        with(retrievedElement.controlImplementations.find { it.control.displayName.endsWith("root control 2") }) {
            implementationStatus == "UNKNOWN"
            responsible.displayName.endsWith("person 1")
            with(owner.get(_requirementImplementations).body) {
                totalItemCount == 2
                with(items.find { it.control.displayName.endsWith("root control 2") }) {
                    status == "UNKNOWN"
                }
                with(items.find { it.control.displayName.endsWith("sub control 3") }) {
                    status == "UNKNOWN"
                }
            }
        }

        when: "editing requirement implementations"
        get("/$elementType.pluralTerm/$elementId/requirement-implementations/$rootControl1Id").with {
            body.status = "PARTIAL"
            body.implementationStatement = "It's a start"
            body.responsible = [targetUri: "/persons/$person2Id"]
            put(body._self, body, getETag(), 204)
        }
        get("/$elementType.pluralTerm/$elementId/requirement-implementations/$subControl2Id").with {
            body.status = "YES"
            body.implementationStatement = "Done!"
            body.responsible = [targetUri: "/persons/$person2Id"]
            put(body._self, body, getETag(), 204)
        }

        then: "changes have been applied"
        with(get("/$elementType.pluralTerm/$elementId/control-implementations/$rootControl1Id/requirement-implementations").body) {
            with(items.find { it.control.displayName.endsWith("root control 1") }) {
                status == "PARTIAL"
                implementationStatement == "It's a start"
                responsible.displayName.endsWith("person 2")
            }
            with(items.find { it.control.displayName.endsWith("sub control 2") }) {
                status == "YES"
                implementationStatement == "Done!"
                responsible.displayName.endsWith("person 2")
            }
        }

        and: "implementation status is reflected in CI"
        get("/$elementType.pluralTerm/$elementId").body
                .controlImplementations
                .find { it.control.displayName.endsWith("root control 1") }
                .implementationStatus == "PARTIAL"

        when: "removing one CI"
        get("/$elementType.pluralTerm/$elementId").with {
            body.controlImplementations.removeIf { it.control.targetUri.endsWith(rootControl1Id) }
            owner.put(body._self, body, getETag())
        }

        then: "it is gone"
        get("/$elementType.pluralTerm/$elementId").body.controlImplementations.size() == 1

        and: "its unedited RIs are gone"
        get("/$elementType.pluralTerm/$elementId/requirement-implementations/$subControl1Id", 404)

        and: "its edited RIs are still present"
        get("/$elementType.pluralTerm/$elementId/requirement-implementations/$rootControl1Id")
        get("/$elementType.pluralTerm/$elementId/requirement-implementations/$subControl2Id")

        and: "RIs for the other CI are still present"
        get("/$elementType.pluralTerm/$elementId/requirement-implementations/$rootControl2Id")
        get("/$elementType.pluralTerm/$elementId/requirement-implementations/$subControl3Id")

        where:
        elementType << EntityType.RISK_AFFECTED_TYPES
    }

    def "CRUD CIs for #elementType.singularTerm with domain-specific API"() {
        given:
        defineSubTypeAndStatus(elementType)

        when: "creating and fetching an element with two CIs"
        def elementId = post("/domains/$domainId/$elementType.pluralTerm", [
            name: "lame",
            subType: "A",
            status: "living",
            owner: [targetUri: "http://localhost/units/$unitId"],
            controlImplementations: [
                [
                    control: [targetUri: "/controls/$rootControl1Id"],
                    description: "I have my reasons",
                ],
                [
                    control: [targetUri: "/controls/$rootControl2Id"],
                    responsible: [targetUri: "/persons/$person1Id"],
                ],
            ]
        ]).body.resourceId
        def retrievedElement = get("/$elementType.pluralTerm/$elementId").body

        then: "CIs for both controls are present"
        retrievedElement.controlImplementations.size() == 2
        with(retrievedElement.controlImplementations.find { it.control.displayName.endsWith("root control 1") }) {
            implementationStatus == "UNKNOWN"
            description == "I have my reasons"
        }
        with(retrievedElement.controlImplementations.find { it.control.displayName.endsWith("root control 2") }) {
            implementationStatus == "UNKNOWN"
            responsible.displayName.endsWith("person 1")
        }

        when: "modifying CIs"
        get("/domains/$domainId/$elementType.pluralTerm/$elementId").with {
            body.controlImplementations.removeIf { it.control.targetUri.endsWith(rootControl1Id) }
            body.controlImplementations
                    .find { it.control.targetUri.endsWith(rootControl2Id) }
                    .description = "I've made changes"
            owner.put(body._self, body, getETag())
        }

        then: "changes have been applied"
        with(get("/domains/$domainId/$elementType.pluralTerm/$elementId").body.controlImplementations) {
            size() == 1
            get(0).control.targetUri.endsWith(owner.rootControl2Id)
            get(0).description == "I've made changes"
        }

        where:
        elementType << EntityType.RISK_AFFECTED_TYPES
    }

    def "responsible person of #elementType.singularTerm CIs & RIs can be deleted"() {
        when: "creating and fetching an element with one CIs and a responsible person"
        def personId = post("/persons", [
            name: "person will be removed",
            owner: [targetUri: "http://localhost/units/$unitId"],
        ]).body.resourceId

        def elementId = post("/$elementType.pluralTerm", [
            name: "lame",
            owner: [targetUri: "http://localhost/units/$unitId"],
            controlImplementations: [
                [
                    control: [targetUri: "/controls/$rootControl1Id"],
                    responsible: [targetUri: "/persons/$personId"],
                ],
            ]
        ]).body.resourceId
        def retrievedElement = get("/$elementType.pluralTerm/$elementId").body

        then: "the person has been assigned to the CI"
        retrievedElement.controlImplementations.size() == 1
        retrievedElement.controlImplementations[0].responsible.targetUri.endsWith("/persons/$personId")

        when: "assigning the RI to the person"
        get("/$elementType.pluralTerm/$elementId/requirement-implementations/$rootControl1Id").with {
            body.responsible = [targetUri: "/persons/$personId"]
            put(body._self, body, getETag(), 204)
        }

        and: "removing the person"
        delete("/persons/$personId")

        and: "fetching the element"
        retrievedElement = get("/$elementType.pluralTerm/$elementId").body

        then: "the responsible person is unset"
        retrievedElement.controlImplementations.size() == 1
        retrievedElement.controlImplementations[0].responsible == null

        get("/$elementType.pluralTerm/$elementId/control-implementations/$rootControl1Id/requirement-implementations").body.responsible == null

        where:
        elementType << EntityType.RISK_AFFECTED_TYPES
    }

    def "concurrent requirement implementation changes on #elementType.singularTerm are detected"() {
        given:
        def elementId = post("/$elementType.pluralTerm", [
            name: "risk aficionado",
            owner: [targetUri: "http://localhost/units/$unitId"],
            controlImplementations: [
                [
                    control: [targetUri: "/controls/$subControl2Id"]
                ]
            ]
        ]).body.resourceId

        when: "retrieving the RI"
        def riUri = "/$elementType.pluralTerm/$elementId/requirement-implementations/$subControl2Id"
        var originalGetResponse = get(riUri)
        var body = originalGetResponse.body
        var originalETag = originalGetResponse.getETag()

        and: "updating it"
        var newETag = put(riUri, body, originalETag, 204).getETag()

        then: "the ETag yielded by the PUT is correct"
        newETag != originalETag
        newETag == get(riUri).getETag()

        and: "the original ETag cannot be used for another update"
        put(riUri, body, originalETag, 412).body.message == "The eTag does not match for the $elementType.singularTerm with the ID $elementId"

        and: "the new ETag can be used for another update"
        put(riUri, body, newETag, 204)

        where:
        elementType << EntityType.RISK_AFFECTED_TYPES
    }

    def "origin of a requirement implementation on #elementType.singularTerm cannot be changed"() {
        given:
        def elementId = post("/$elementType.pluralTerm", [
            name: "protagonist",
            owner: [targetUri: "http://localhost/units/$unitId"],
            controlImplementations: [
                [
                    control: [targetUri: "/controls/$subControl2Id"]
                ]
            ]
        ]).body.resourceId
        def otherElementId = post("/assets", [
            name: "antagonist",
            owner: [targetUri: "http://localhost/units/$unitId"]
        ]).body.resourceId

        when: "altering the origin of the RI"
        get("/$elementType.pluralTerm/$elementId/requirement-implementations/$subControl2Id").with{
            body.origin.targetUri = "/assets/$otherElementId"
            put(body._self, body, getETag(), 204)
        }

        then: "the origin remains the same"
        get("/$elementType.pluralTerm/$elementId/requirement-implementations/$subControl2Id")
                .body.origin.targetUri.endsWith("/$elementType.pluralTerm/$elementId")

        where:
        elementType << EntityType.RISK_AFFECTED_TYPES
    }

    def "control of a requirement implementation on #elementType.singularTerm cannot be changed"() {
        given:
        def elementId = post("/$elementType.pluralTerm", [
            name: "risk aficionado",
            owner: [targetUri: "http://localhost/units/$unitId"],
            controlImplementations: [
                [
                    control: [targetUri: "/controls/$subControl2Id"]
                ]
            ]
        ]).body.resourceId

        when: "altering the control of the RI"
        get("/$elementType.pluralTerm/$elementId/requirement-implementations/$subControl2Id").with{
            body.control.targetUri = "/controls/$subControl3Id"
            put(body._self, body, getETag(), 204)
        }

        then: "the control remains the same"
        get("/$elementType.pluralTerm/$elementId/requirement-implementations/$subControl2Id")
                .body.control.targetUri.endsWith("/controls/$subControl2Id")

        where:
        elementType << EntityType.RISK_AFFECTED_TYPES
    }

    def "cannot create control implementation for control from another unit"() {
        given: "another unit"
        def otherUnitId = postNewUnit().resourceId

        expect: "that a control implementation cannot be created there referencing a control in the main unit"
        post("/$elementType.pluralTerm", [
            name: "risk influencer",
            owner: [targetUri: "http://localhost/units/$otherUnitId"],
            controlImplementations: [
                [
                    control: [targetUri: "/controls/$rootControl1Id"]
                ]
            ]
        ], 422).body.message == "Elements in other units must not be referenced"

        where:
        elementType << EntityType.RISK_AFFECTED_TYPES
    }

    def "cannot assign person from another unit as responsible for control implementation"() {
        given: "a person in another unit"
        def otherUnitId = postNewUnit().resourceId
        def personId = post("/persons", [
            name: "person in other unit",
            owner: [targetUri: "/units/$otherUnitId"]
        ]).body.resourceId

        expect: "that it cannot be assigned as a responsible person in this unit"
        post("/$elementType.pluralTerm", [
            name: "risk influencer",
            owner: [targetUri: "http://localhost/units/$unitId"],
            controlImplementations: [
                [
                    control: [targetUri: "/controls/$rootControl1Id"],
                    responsible: [targetUri: "/persons/$personId"],
                ]
            ]
        ], 422).body.message == "Elements in other units must not be referenced"

        where:
        elementType << EntityType.RISK_AFFECTED_TYPES
    }

    def "cannot assign person from another unit as responsible for requirement implementation"() {
        given: "a control implementation"
        def elementId = post("/$elementType.pluralTerm", [
            name: "risk influencer",
            owner: [targetUri: "http://localhost/units/$unitId"],
            controlImplementations: [
                [
                    control: [targetUri: "/controls/$rootControl1Id"]
                ]
            ]
        ]).body.resourceId

        and: "a person in another unit"
        def otherUnitId = postNewUnit().resourceId
        def personId = post("/persons", [
            name: "person in other unit",
            owner: [targetUri: "/units/$otherUnitId"]
        ]).body.resourceId

        expect: "that the person cannot be added as an RI responsible"
        get("/$elementType.pluralTerm/$elementId/requirement-implementations/$rootControl1Id").with{
            body.responsible = [targetUri: "/persons/$personId"]
            put(body._self, body, getETag(), 422)
        }.body.message == "Elements in other units must not be referenced"

        where:
        elementType << EntityType.RISK_AFFECTED_TYPES
    }

    def "missing resources are handled for #elementType.pluralTerm"() {
        given:
        var randomUuid = UUID.randomUUID().toString()
        def elementId = post("/$elementType.pluralTerm", [
            name: "risk affe",
            owner: [targetUri: "http://localhost/units/$unitId"],
            controlImplementations: [
                [
                    control: [targetUri: "/controls/$subControl2Id"]
                ]
            ]
        ]).body.resourceId

        expect:
        get("/$elementType.pluralTerm/$randomUuid/control-implementations/$subControl2Id/requirement-implementations", 404)
                .body.message == "$elementType.type.simpleName with ID $randomUuid not found"
        get("/$elementType.pluralTerm/$elementId/control-implementations/$randomUuid/requirement-implementations", 404)
                .body.message == "Control with ID $randomUuid not found"
        get("/$elementType.pluralTerm/$elementId/control-implementations/$subControl3Id/requirement-implementations", 404)
                .body.message == "$elementType.singularTerm $elementId does not implement control $subControl3Id"

        and:
        get("/$elementType.pluralTerm/$elementId/requirement-implementations/$randomUuid", 404)
                .body.message == "Control with ID $randomUuid not found"
        get("/$elementType.pluralTerm/$elementId/requirement-implementations/$subControl3Id", 404)
                .body.message == "$elementType.singularTerm $elementId contains no requirement implementation for control $subControl3Id"

        and:
        put("/$elementType.pluralTerm/$elementId/requirement-implementations/$randomUuid", [
            origination: [targetUri: "/$elementType.pluralTerm/$elementId"],
            control: [targetUri: "/controls/$subControl2Id"],
            status: "YES",
            origination: "SYSTEM_SPECIFIC",
        ], "", 404)
        .body.message == "Control with ID $randomUuid not found"
        put("/$elementType.pluralTerm/$elementId/requirement-implementations/$subControl3Id", [
            origination: [targetUri: "/$elementType.pluralTerm/$elementId"],
            control: [targetUri: "/controls/$subControl2Id"],
            status: "YES",
            origination: "SYSTEM_SPECIFIC",
        ], "", 404)
        .body.message == "$elementType.singularTerm $elementId contains no requirement implementation for control $subControl3Id"

        where:
        elementType << EntityType.RISK_AFFECTED_TYPES
    }

    String defineSubTypeAndStatus(EntityType type) {
        put("/content-creation/domains/$domainId/element-type-definitions/${type.singularTerm}", [
            subTypes: [
                A: [
                    statuses: [
                        "living"
                    ]
                ]
            ]
        ], null, 204)
    }
}
