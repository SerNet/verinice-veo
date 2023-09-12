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
    private String unitId
    private String rootControl1Id
    private String rootControl2Id
    private String subControl1Id
    private String subControl2Id
    private String subControl3Id
    private String person1Id
    private String person2Id

    def setup() {
        unitId = postNewUnit()

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
        var getResponse = get("/$elementType.pluralTerm/$elementId/requirement-implementations/$subControl2Id")
        getResponse.body.origin.targetUri = "/assets/$otherElementId"

        then: "the change cannot be persisted"
        put(getResponse.body._self, getResponse.body, getResponse.getETag(), 422).body.message == "Property 'origin' is read-only and cannot be modified"

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
        var getResponse = get("/$elementType.pluralTerm/$elementId/requirement-implementations/$subControl2Id")
        getResponse.body.control.targetUri = "/controls/$subControl3Id"

        then: "the change cannot be persisted"
        put(getResponse.body._self, getResponse.body, getResponse.getETag(), 422).body.message == "Property 'control' is read-only and cannot be modified"

        where:
        elementType << EntityType.RISK_AFFECTED_TYPES
    }
}
