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

import org.springframework.http.MediaType

import org.veo.rest.UnitController

class UnitImportRestTest extends VeoRestTest {

    def "unit export supports v2 structure via media type"() {
        given:
        def unitId = postNewUnit().resourceId
        post("/domains/$testDomainId/processes", [
            name: "Versioned export process",
            owner: [targetUri: "/units/$unitId"],
            subType: "BusinessProcess",
            status: "NEW"
        ]).body.resourceId

        when:
        def exportV1 = get("/units/$unitId/export").body
        def exportV2 = get(
                "/units/$unitId/export",
                200,
                UserType.DEFAULT,
                MediaType.parseMediaType(UnitController.MEDIA_TYPE_UNIT_DUMP_V2_VALUE)
                ).body
        def processV1 = exportV1.elements.find { it.type == 'process' }
        def processV2 = exportV2.elements.find { it.type == 'process' }

        then:
        processV1 != null
        processV1.customAspects != null
        processV1.links != null

        and: "v2 moves customizations into domain associations"
        with(processV2) {
            customAspects == null
            links == null
            domains.size() >= 1
            domains.values().every { it.customAspects != null }
            domains.values().every { it.links != null }
        }

        and: "v2 domain export only contains metadata"
        with(exportV2.domains.find { it.name == "test-domain" }) {
            templateVersion != null
            !containsKey('elementTypeDefinitions') || elementTypeDefinitions == [:]
            !containsKey('riskDefinitions') || riskDefinitions == [:]
        }
    }

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
        def unitId = post("/units/import", unitBackup).body.resourceId

        then: "it's back"
        with(get("/units/$unitId").body) {
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

    def "export a unit from the default client and import it for the secondary client"() {
        given: "a unit in the default client with a scenario"
        def unitUri = post("/units", [
            name: "Cross client unit",
            domains: [
                [targetUri: "/domains/$testDomainId"],
                [targetUri: "/domains/$dsgvoDomainId"],
            ]
        ]).location
        def scenarioId = post("/domains/$testDomainId/scenarios", [
            name: "Cross client scenario",
            abbreviation: "CCS",
            description: "A scenario created in the default client",
            owner: [targetUri: unitUri],
            subType: "Attack",
            status: "NEW",
        ]).body.resourceId

        when: "exporting the unit from the default client"
        def exportedUnit = get("$unitUri/export").body

        and: "importing the unit into the secondary client"
        def secondaryDomains = get("/domains", 200, UserType.SECONDARY_CLIENT_USER).body
        def secondaryTestDomainId = secondaryDomains.find { it.name == "test-domain" }.id
        def secondaryDsgvoDomainId = secondaryDomains.find { it.name == "DS-GVO" }.id
        def importedUnitUri = post("/units/import", exportedUnit, 201, UserType.SECONDARY_CLIENT_USER).location
        def importedUnitId = (importedUnitUri =~ /\/units\/(.+)/)[0][1]

        then: "the imported unit is created in the secondary client"
        with(get("/units/$importedUnitId", 200, UserType.SECONDARY_CLIENT_USER).body) {
            name == "Cross client unit"
            domains.size() == 2
            domains.any { it.targetUri.endsWith("/domains/$secondaryTestDomainId") }
            domains.any { it.targetUri.endsWith("/domains/$secondaryDsgvoDomainId") }
        }

        and: "the scenario is restored in the secondary client"
        def importedScenario = get("/domains/$secondaryTestDomainId/scenarios?unit=$importedUnitId", 200, UserType.SECONDARY_CLIENT_USER).body.items[0]
        with(get("/domains/$secondaryTestDomainId/scenarios/${importedScenario.id}", 200, UserType.SECONDARY_CLIENT_USER).body) {
            name == "Cross client scenario"
            abbreviation == "CCS"
            status == "NEW"
            subType == "Attack"
        }
    }

    def "import fails if domain version does not match"() {
        given: "a unit exported with a specific domain version"
        def unitUri = post("/units", [
            name: "Version mismatch unit",
            domains: [
                [targetUri: "/domains/$testDomainId"]
            ]
        ]).location

        def exportedUnit = get("$unitUri/export").body
        exportedUnit.domains[0].templateVersion = "9999.0.0"

        when: "importing into the secondary client"
        def response = post("/units/import", exportedUnit, 422, UserType.SECONDARY_CLIENT_USER)

        then: "a helpful version mismatch error is returned"
        '''Domain 'test-domain' (authority: SERNET) exists in the target client but not in version 9999.0.0. Available versions: [1.0.0].'''
    }

    def "export a unit from the default client and import it with risks for the secondary client"() {
        given: "a unit with a process risk in the default client"
        def unitUri = post("/units", [
            name: "Cross client risk unit",
            domains: [
                [targetUri: "/domains/$testDomainId"],
            ]
        ]).location
        def scenarioId = post("/domains/$testDomainId/scenarios", [
            name: "Cross client scenario",
            owner: [targetUri: unitUri],
            subType: "Attack",
            status: "NEW",
        ]).body.resourceId
        def processId = post("/domains/$testDomainId/processes", [
            name: "Cross client process",
            owner: [targetUri: unitUri],
            subType: "BusinessProcess",
            status: "NEW"
        ]).body.resourceId
        post("/processes/$processId/risks", [
            scenario: [targetUri: "/scenarios/$scenarioId"],
            domains: [
                (testDomainId): [
                    reference: [targetUri: "/domains/$testDomainId"],
                    riskDefinitions: [
                        riskyDef: [
                            probability: [
                                specificProbability: 2
                            ]
                        ]
                    ]
                ]
            ]
        ])

        when: "exporting from the default client and importing into the secondary client"
        def exportedUnit = get("$unitUri/export").body
        def secondaryTestDomainId = get("/domains", 200, UserType.SECONDARY_CLIENT_USER).body.find { it.name == "test-domain" }.id
        def importedUnitUri = post("/units/import", exportedUnit, 201, UserType.SECONDARY_CLIENT_USER).location
        def importedUnitId = (importedUnitUri =~ /\/units\/(.+)/)[0][1]

        then: "the risk is imported with its domain correctly remapped to the secondary client's domain"
        def importedProcessId = get("/domains/$secondaryTestDomainId/processes?unit=$importedUnitId", 200, UserType.SECONDARY_CLIENT_USER).body.items[0].id
        def importedScenarioId = get("/domains/$secondaryTestDomainId/scenarios?unit=$importedUnitId", 200, UserType.SECONDARY_CLIENT_USER).body.items[0].id
        with(get("/processes/$importedProcessId/risks/$importedScenarioId", 200, UserType.SECONDARY_CLIENT_USER).body) {
            domains[secondaryTestDomainId] != null
            domains[secondaryTestDomainId].riskDefinitions.riskyDef.probability.specificProbability == 2
        }
    }

    def "import fails if domain is missing in target client"() {
        given: "a unit exported with a domain"
        def unitUri = post("/units", [
            name: "Missing domain unit",
            domains: [
                [targetUri: "/domains/$testDomainId"]
            ]
        ]).location

        def exportedUnit = get("$unitUri/export").body
        exportedUnit.domains[0].name = "missing-domain"
        exportedUnit.domains[0].authority = "missing-authority"

        when: "importing into the secondary client"
        def response = post("/units/import", exportedUnit, 422, UserType.SECONDARY_CLIENT_USER)

        then: "a helpful error is returned"
        response.body.message == "Domain 'missing-domain' (authority: missing-authority) is not available in the target client."
    }

    def "existing resources are not modified"() {
        given: "a unit with a document"
        def oldUnitUri = post("/units", [
            name: "old unit name",
            domains:[
                [targetUri: "/domains/$testDomainId"]
            ]
        ]).location
        def oldDocumentUri = post("/domains/$testDomainId/documents", [
            name: "old document name",
            subType: 'Manual',
            status: 'CURRENT',
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
            ],
            translations : [(EN):
                ["control_Ctl_plural":"asset_AST_Application_plural",
                    "control_Ctl_singular":"asset_AST_Application_singular",
                    "control_Ctl_status_NEW":"asset_AST_Application_status_NEW",
                ]
            ]
        ], null, 204, UserType.CONTENT_CREATOR)

        and: "a unit with a control"
        def unitId = postNewUnit("U1", [domainId]).resourceId
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
            translations.en.put("control_Ctl_status_OUT_OF_CONTROL", "control_Ctl_status_OUT_OF_CONTROL")
            translations.en.remove("control_Ctl_status_NEW")
            put("/content-creation/domains/$domainId/element-type-definitions/control", it, null, 204, UserType.CONTENT_CREATOR)
        }

        then: "the exported unit cannot be imported"
        with(post("/units/import", exportedUnit, 422).body) {
            message == "Status 'NEW' is not allowed for subtype 'Ctl'"
        }

        when: "fixing the control's status in the exported unit"
        exportedUnit.elements[0].domains[domainId].status = "OUT_OF_CONTROL"

        then: "it can be imported"
        post("/units/import", exportedUnit)
    }

    def "unit import rejects conflicting shared custom aspect values across domains"() {
        given: "two domains with identical process custom aspect definitions"
        def domainIdA = post("/content-creation/domains", [
            name: "import conflict domain A ${UUID.randomUUID()}",
            authority: "jj",
            translations: [en:[name:"Import conflict domain A"]]
        ], 201, UserType.CONTENT_CREATOR).body.resourceId
        def domainIdB = post("/content-creation/domains", [
            name: "import conflict domain B ${UUID.randomUUID()}",
            authority: "jj",
            translations: [en:[name:"Import conflict domain B"]]
        ], 201, UserType.CONTENT_CREATOR).body.resourceId

        put("/content-creation/domains/$domainIdA/element-type-definitions/process", [
            subTypes: [
                ProcA: [
                    statuses: ["NEW", "OLD"]
                ]
            ],
            customAspects: [
                sharedCa: [
                    attributeDefinitions: [
                        someAttr: [type: "integer"]
                    ]
                ]
            ],
            translations : [
                (EN):["process_ProcA_plural":"process_ProcA_plural",
                    "process_ProcA_singular": "process_ProcA_singular",
                    "process_ProcA_status_NEW" : "process_ProcA_status_NEW",
                    "process_ProcA_status_OLD" : "process_ProcA_status_OLD",
                    "someAttr" : "someAttr",
                ]
            ]
        ], null, 204, UserType.CONTENT_CREATOR)
        put("/content-creation/domains/$domainIdB/element-type-definitions/process", [
            subTypes: [
                ProcB: [
                    statuses: ["ON", "OFF"]
                ]
            ],
            customAspects: [
                sharedCa: [
                    attributeDefinitions: [
                        someAttr: [type: "integer"]
                    ]
                ]
            ],
            translations : [
                (EN):["process_ProcB_plural":"process_ProcA_plural",
                    "process_ProcB_singular": "process_ProcA_singular",
                    "process_ProcB_status_ON" : "process_ProcA_status_NEW",
                    "process_ProcB_status_OFF" : "process_ProcA_status_OLD",
                    "someAttr" : "someAttr",
                ]
            ]
        ], null, 204, UserType.CONTENT_CREATOR)

        and: "a process associated with both domains"
        def unitId = postNewUnit("conflict import unit", [domainIdA, domainIdB]).resourceId
        def processId = post("/domains/$domainIdA/processes", [
            name: "conflict process",
            owner: [targetUri: "/units/$unitId"],
            subType: "ProcA",
            status: "NEW"
        ]).body.resourceId
        post("/domains/$domainIdB/processes/$processId", [
            subType: "ProcB",
            status: "ON"
        ], 200)

        and: "an export payload in old format"
        def exportedUnit = get("/units/$unitId/export").body
        def process = exportedUnit.elements.find { it.type == "process" }

        and: "conflicting shared custom aspect values in different domains"
        process.domains[domainIdA].customAspects = [
            sharedCa: [
                someAttr: 1
            ]
        ]
        process.domains[domainIdB].customAspects = [
            sharedCa: [
                someAttr: 2
            ]
        ]

        when: "importing the manipulated payload"
        def response = post("/units/import", exportedUnit, 422)

        then: "import is rejected with a conflict message"
        response.body.message.contains("Import conflict domain A")
        response.body.message.contains("Import conflict domain B")
        response.body.message.contains("the object has deviating values in those domains")
    }

    def "unit import supports new structure for element custom aspects and links"() {
        given: "a unit with a process that has custom aspects and links"
        def oldUnitId = postNewUnit("new structure import unit", [dsgvoDomainId]).resourceId
        def oldUnitUri = "/units/$oldUnitId"
        def targetAssetId = post("/domains/$dsgvoDomainId/assets", [
            name: "target asset for new structure import",
            subType: "AST_Datatype",
            status: "NEW",
            owner: [targetUri: oldUnitUri],
        ]).body.resourceId
        post("/domains/$dsgvoDomainId/processes", [
            customAspects: [
                process_dataProcessing: [
                    process_dataProcessing_legalBasis: [
                        "process_dataProcessing_legalBasis_Art6Abs1liteDSGVO",
                        "process_dataProcessing_legalBasis_Art6Abs1litbDSGVO"
                    ]
                ]
            ],
            links: [
                process_dataType: [
                    [
                        target: [
                            targetUri: "$baseUrl/assets/$targetAssetId"
                        ]
                    ]
                ]
            ],
            name: "new-structure import process",
            owner: [targetUri: oldUnitUri],
            subType: "PRO_DataProcessing",
            status: "NEW"
        ])

        and: "a v2 export"
        def exportedUnit = get(
                "$oldUnitUri/export",
                200,
                UserType.DEFAULT,
                MediaType.parseMediaType(UnitController.MEDIA_TYPE_UNIT_DUMP_V2_VALUE)
                ).body

        when: "importing the v2 payload"
        def newUnitId = post("/units/import", exportedUnit).body.resourceId
        def importedProcessId = get("/domains/$dsgvoDomainId/processes?unit=$newUnitId").body.items[0].id
        def importedAssetId = get("/domains/$dsgvoDomainId/assets?unit=$newUnitId").body.items[0].id
        def importedProcess = get("/domains/$dsgvoDomainId/processes/$importedProcessId").body

        then: "import preserves custom aspects and links"
        newUnitId != null
        importedProcess.customAspects.process_dataProcessing.process_dataProcessing_legalBasis as Set == [
            "process_dataProcessing_legalBasis_Art6Abs1liteDSGVO",
            "process_dataProcessing_legalBasis_Art6Abs1litbDSGVO"
        ] as Set
        importedProcess.links.process_dataType[0].target.targetUri.endsWith("/assets/$importedAssetId")
    }

    def "unit import with multipart supports new structure for element custom aspects and links"() {
        given: "a unit with a process that has custom aspects and links"
        def oldUnitId = postNewUnit("new structure multipart import unit", [dsgvoDomainId]).resourceId
        def oldUnitUri = "/units/$oldUnitId"
        def targetAssetId = post("/domains/$dsgvoDomainId/assets", [
            name: "target asset for new structure multipart import",
            subType: "AST_Datatype",
            status: "NEW",
            owner: [targetUri: oldUnitUri],
        ]).body.resourceId
        post("/domains/$dsgvoDomainId/processes", [
            customAspects: [
                process_dataProcessing: [
                    process_dataProcessing_legalBasis: [
                        "process_dataProcessing_legalBasis_Art6Abs1liteDSGVO",
                        "process_dataProcessing_legalBasis_Art6Abs1litbDSGVO"
                    ]
                ]
            ],
            links: [
                process_dataType: [
                    [
                        target: [
                            targetUri: "$baseUrl/assets/$targetAssetId"
                        ]
                    ]
                ]
            ],
            name: "new-structure multipart import process",
            owner: [targetUri: oldUnitUri],
            subType: "PRO_DataProcessing",
            status: "NEW"
        ])

        and: "a v2 export"
        def exportedUnit = get(
                "$oldUnitUri/export",
                200,
                UserType.DEFAULT,
                MediaType.parseMediaType(UnitController.MEDIA_TYPE_UNIT_DUMP_V2_VALUE)
                ).body

        when: "importing the v2 payload via multipart"
        def newUnitId = postMultipart("/units/import", exportedUnit).body.resourceId
        def importedProcessId = get("/domains/$dsgvoDomainId/processes?unit=$newUnitId").body.items[0].id
        def importedAssetId = get("/domains/$dsgvoDomainId/assets?unit=$newUnitId").body.items[0].id
        def importedProcess = get("/domains/$dsgvoDomainId/processes/$importedProcessId").body

        then: "multipart import preserves custom aspects and links"
        importedProcess.customAspects.process_dataProcessing.process_dataProcessing_legalBasis as Set == [
            "process_dataProcessing_legalBasis_Art6Abs1liteDSGVO",
            "process_dataProcessing_legalBasis_Art6Abs1litbDSGVO"
        ] as Set
        importedProcess.links.process_dataType[0].target.targetUri.endsWith("/assets/$importedAssetId")
    }
}
