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

    def "CRUD control implementations for #elementType.singularTerm"() {
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

        when: "removing one CI"
        get("/$elementType.pluralTerm/$elementId").with {
            body.controlImplementations.removeIf { it.control.targetUri.endsWith(rootControl1Id) }
            owner.put(body._self, body, getETag())
        }

        then: "it is gone"
        get("/$elementType.pluralTerm/$elementId").body.controlImplementations.size() == 1

        where:
        elementType << EntityType.RISK_AFFECTED_TYPES
    }
}
