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

class UnitImportRestTest extends VeoRestTest {

    def "export, delete and import a unit"() {
        given:
        def unitUri = post("/units", [
            name: "Lost Unit",
            abbreviation: "LU",
            description: "It used to be a flourishing unit inhabited by beautiful elements, until it was deleted by a great disaster",
            domains: [
                [targetUri: "/domains/$testDomainId"],
                [targetUri: "/domains/$dsgvoDomainId"],
            ]
        ]).location
        def scenarioId = post("/domains/$testDomainId/scenarios", [
            name: "Accidental unit deletion",
            abbreviation: "AUD",
            description: "Somebody accidentally deletes the whole unit",
            owner: [targetUri: unitUri],
            subType: "Attack",
            status: "NEW",
        ]).body.resourceId
        def controlId = post("/domains/$testDomainId/controls", [
            name: "Backup",
            abbreviation: "BU",
            description: "Backup your unit using the unit export feature",
            owner: [targetUri: unitUri],
            subType: "TOM",
            status: "OLD",
        ]).body.resourceId
        def processId = post("/domains/$testDomainId/processes", [
            name: "Important process",
            owner: [targetUri: unitUri],
            subType: "BusinessProcess",
            status: "NEW"
        ]).body.resourceId
        post("/processes/$processId/risks", [
            scenario: [targetUri: "/scenarios/$scenarioId"],
            mitigation: [targetUri: "/controls/$controlId"],
            domains: [
                (testDomainId): [
                    reference: [targetUri: "/domains/$testDomainId"],
                    riskDefinitions: [
                        riskyDef: [
                            probability: [
                                specificProbability: 2,
                                specificProbabilityExplanation: "I have a feeling that somebody is going to delete the whole unit soon"
                            ]
                        ]
                    ]
                ]
            ]
        ])

        when: "exporting the unit as a backup"
        def unitBackup = get("$unitUri/export").body

        and: "deleting the unit"
        delete(unitUri)

        then: "everything is gone"
        get(unitUri, 404)
        get("/scenarios/$scenarioId", 404)
        get("/controls/$controlId", 404)
        get("/processes/$processId", 404)

        when: "reviving the unit by importing the backup"
        unitUri = post("/units/import", unitBackup).location
        def unitId = (unitUri =~ /\/units\/(.+)/)[0][1]

        then: "it's back"
        with(get(unitUri).body) {
            name == "Lost Unit"
            abbreviation == "LU"
            description =~ /It used to be a .*/
            domains.size() == 2
            domains.any { it.targetUri.endsWith("/domains/$owner.owner.dsgvoDomainId") }
            domains.any { it.targetUri.endsWith("/domains/$owner.owner.testDomainId") }
        }

        when: "looking up the restored elements"
        controlId = get("/domains/$testDomainId/controls?unit=$unitId").body.items[0].id
        processId = get("/domains/$testDomainId/processes?unit=$unitId").body.items[0].id
        scenarioId = get("/domains/$testDomainId/scenarios?unit=$unitId").body.items[0].id

        then: "all the elements are back"
        with(get("/domains/$testDomainId/scenarios/$scenarioId").body) {
            name == "Accidental unit deletion"
            abbreviation == "AUD"
            description =~ /Somebody accidentally deletes the .*/
            subType == "Attack"
            status == "NEW"
        }
        with(get("/domains/$testDomainId/controls/$controlId").body) {
            name == "Backup"
            abbreviation == "BU"
            description =~ /Backup your unit .*/
            subType == "TOM"
            status == "OLD"
        }
        with(get("/domains/$testDomainId/processes/$processId").body) {
            name == "Important process"
            subType == "BusinessProcess"
            status == "NEW"
        }

        and: "the risk is also back"
        with(get("/processes/$processId/risks/$scenarioId").body) {
            mitigation.targetUri.endsWith("/controls/$controlId")
        }
    }

    def "export, delete and import a unit with content type multipart"() {
        given:
        def name = "Lost Multipart Unit"
        def abbreviation = "LMPU"
        def unitUri = post("/units", [
            name: "$name",
            abbreviation: "$abbreviation",
            description: "It used to be a flourishing unit inhabited by beautiful elements, until it was deleted by a great disaster",
            domains: [
                [targetUri: "/domains/$testDomainId"],
                [targetUri: "/domains/$dsgvoDomainId"],
            ]
        ]).location

        when: "exporting the unit as a backup"
        def unitBackup = get("$unitUri/export").body

        and: "deleting the unit"
        delete(unitUri)

        and: "reviving the unit by importing the backup with content type multipart"
        unitUri = postMultipart("/units/import", unitBackup).location

        then: "it's back"
        with(get(unitUri).body) {
            name == "$name"
            abbreviation == "$abbreviation"
            description =~ /It used to be a .*/
            domains.size() == 2
            domains.any { it.targetUri.endsWith("/domains/$owner.owner.dsgvoDomainId") }
            domains.any { it.targetUri.endsWith("/domains/$owner.owner.testDomainId") }
        }
    }

    def "existing resources are not modified"() {
        given: "a unit with a document"
        def oldUnitUri = post("/units", [
            name: "old unit name"
        ]).location
        def oldDocumentUri = post("/documents", [
            name: "old document name",
            owner: [targetUri: oldUnitUri],
        ]).location

        when: "exporting the unit"
        def exportedUnit = get("$oldUnitUri/export").body

        and: "manipulating the exported unit"
        exportedUnit.unit.name = "new unit name"
        exportedUnit.elements[0].name = "new document name"

        and: "importing the manipulated unit"
        def newUnitUri = post("/units/import", exportedUnit).location
        def newUnitId = (newUnitUri =~ /\/units\/(.+)/)[0][1]
        def newDocumentUri = get("/documents?unit=$newUnitId").body.items[0]._self

        then: "old resources have not been modified"
        get(oldUnitUri).body.name == "old unit name"
        get(oldDocumentUri).body.name == "old document name"

        and: "new resources have new values"
        get(newUnitUri).body.name == "new unit name"
        get(newDocumentUri).body.name == "new document name"
    }

    // TODO VEO-867 assert correct catalog item references once element DTOs contain applied catalog item references.
    def "incarnated catalog items are tolerated"() {
        given: "an incarnated catalog item"
        def oldUnitUri = "/units/" + postNewUnit().resourceId
        def catalogItemId = get("/domains/$dsgvoDomainId/catalog-items?size=100").body.items.find { it.abbreviation == "DS-G.38" }.id
        def incarnationDescriptions = get("$oldUnitUri/domains/$dsgvoDomainId/incarnation-descriptions?itemIds=$catalogItemId").body
        post("$oldUnitUri/incarnations", incarnationDescriptions)

        expect: "unit export and import to succeed"
        def exportedUnit = get("$oldUnitUri/export").body
        post("/units/import", exportedUnit)
    }

    def "incompatible elements are rejected"() {
        given: "a domain with a control sub type"
        def domainId = post("/content-creation/domains", [
            name: "import rest test domain ${UUID.randomUUID()}",
            authority: "jj",
        ], 201, UserType.CONTENT_CREATOR).body.resourceId
        put("/content-creation/domains/$domainId/element-type-definitions/control", [
            subTypes: [
                Ctl: [
                    statuses: ["NEW"]
                ]
            ]
        ], null, 204, UserType.CONTENT_CREATOR)

        and: "a unit with a control"
        def unitId = postNewUnit().resourceId
        post("/domains/$domainId/controls", [
            name: "control patrol",
            owner: [targetUri: "/units/$unitId"],
            subType: "Ctl",
            status: "NEW"
        ])

        when: "exporting the unit"
        def exportedUnit = get("/units/$unitId/export").body

        and: "renaming the status in the domain"
        get("/domains/$domainId").body.elementTypeDefinitions.control.with {
            subTypes.Ctl.statuses[0] = "OUT_OF_CONTROL"
            put("/content-creation/domains/$domainId/element-type-definitions/control", it, null, 204, UserType.CONTENT_CREATOR)
        }

        then: "the exported unit cannot be imported"
        with(post("/units/import", exportedUnit, 422).body) {
            message == "Status 'NEW' is not allowed for sub type 'Ctl'"
        }

        when: "fixing the control's status in the exported unit"
        exportedUnit.elements[0].domains[domainId].status = "OUT_OF_CONTROL"

        then: "it can be imported"
        post("/units/import", exportedUnit)
    }
}
