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

import org.veo.core.entity.ElementType

/**
 * REST tests for the feature "CAs für CIs benutzen" (Custom Aspects for Control Implementations).
 * Covers reading and writing of CI custom aspects via GET/PUT /domains/{domainId}/{riskAffectedType}/{riskAffectedId}.
 */
class ControlImplementationCustomAspectsRestTest extends VeoRestTest {

    private String domainId
    private String domain2Id
    private String unitId
    private String controlId

    def setup() {
        domainId = post("/content-creation/domains", [
            name: "CI CAs test domain ${UUID.randomUUID()}",
            authority: "The king",
        ], 201, CONTENT_CREATOR).body.resourceId
        domain2Id = post("/content-creation/domains", [
            name: "CI CAs test domain 2 ${UUID.randomUUID()}",
            authority: "The empress",
        ], 201, CONTENT_CREATOR).body.resourceId

        put("/content-creation/domains/$domainId/element-type-definitions/control", [
            subTypes: [ComplCtl: [statuses: ["NEW"]]],
        ], null, 204, CONTENT_CREATOR)
        put("/content-creation/domains/$domain2Id/element-type-definitions/control", [
            subTypes: [ComplCtl: [statuses: ["NEW"]]],
        ], null, 204, CONTENT_CREATOR)

        put("/content-creation/domains/$domainId/control-implementation-configuration", [
            complianceControlSubTypes: ["ComplCtl"],
        ], null, 204, CONTENT_CREATOR)
        put("/content-creation/domains/$domain2Id/control-implementation-configuration", [
            complianceControlSubTypes: ["ComplCtl"],
        ], null, 204, CONTENT_CREATOR)

        ElementType.RISK_AFFECTED_TYPES.each { type->
            setupRiskAffectedType(type.getSingularTerm())
        }

        unitId = postNewUnit("U1", [domainId, domain2Id]).resourceId

        controlId = post("/domains/$domainId/controls", [
            name: "Test control",
            subType: "ComplCtl",
            status: "NEW",
            owner: [targetUri: "http://localhost/units/$unitId"],
        ]).body.resourceId
    }

    def setupRiskAffectedType(String type) {

        put("/content-creation/domains/$domainId/element-type-definitions/$type", [
            subTypes: [A: [statuses: ["NEW"]]],
        ], null, 204, CONTENT_CREATOR)
        put("/content-creation/domains/$domain2Id/element-type-definitions/$type", [
            subTypes: [A: [statuses: ["NEW"]]],
        ], null, 204, CONTENT_CREATOR)

        put("/content-creation/domains/$domainId/element-type-definitions/$type/control-implementation", [
            customAspects: [
                ciDetails: [
                    attributeDefinitions: [
                        implementationNotes: [type: "text"],
                        priority: [type: "text"],
                    ]
                ]
            ]
        ], null, 204, CONTENT_CREATOR)

        put("/content-creation/domains/$domain2Id/element-type-definitions/$type/control-implementation", [
            customAspects: [
                otherCa: [
                    attributeDefinitions: [
                        reviewDate: [type: "text"],
                    ]
                ]
            ]
        ], null, 204, CONTENT_CREATOR)
    }

    def "can add custom aspects to a control implementation for #elementType.singularTerm"() {
        given: "an element with a control implementation"
        def elementId = post("/domains/$domainId/$elementType.pluralTerm", [
            name: "element with CI",
            subType: "A",
            status: "NEW",
            owner: [targetUri: "http://localhost/units/$unitId"],
            controlImplementations: [
                [
                    control: [targetUri: "/controls/$controlId"],
                    description: "We implement this control",
                ],
            ],
        ]).body.resourceId

        when: "adding custom aspects to the CI via domain element PUT"
        def getResponse = get("/domains/$domainId/$elementType.pluralTerm/$elementId")
        def body = getResponse.body
        body.controlImplementations[0].customAspects = [
            ciDetails: [
                implementationNotes: "Some notes",
                priority: "high",
            ],
        ]
        put("/domains/$domainId/$elementType.pluralTerm/$elementId", body, getResponse.getETag())

        then: "the custom aspects are persisted and returned on GET"
        with(get("/domains/$domainId/$elementType.pluralTerm/$elementId").body) {
            controlImplementations.size() == 1
            with(controlImplementations[0].customAspects) {
                ciDetails != null
                ciDetails.implementationNotes == "Some notes"
                ciDetails.priority == "high"
            }
        }

        where:
        elementType << ElementType.RISK_AFFECTED_TYPES
    }

    def "cannot add custom aspects that are not defined in the domain for #elementType.singularTerm"() {
        given: "an element with a control implementation"
        def elementId = post("/domains/$domainId/$elementType.pluralTerm", [
            name: "element with CI",
            subType: "A",
            status: "NEW",
            owner: [targetUri: "http://localhost/units/$unitId"],
            controlImplementations: [
                [
                    control: [targetUri: "/controls/$controlId"],
                ],
            ],
        ]).body.resourceId

        when: "adding a custom aspect type that is not defined for CIs in this domain"
        def getResponse = get("/domains/$domainId/$elementType.pluralTerm/$elementId")
        def body = getResponse.body
        body.controlImplementations[0].customAspects = [
            undefinedCaType: [
                someAttr: "value",
            ],
        ]
        def response = put("/domains/$domainId/$elementType.pluralTerm/$elementId", body, getResponse.getETag(), 422)

        then: "the request is rejected with a clear message"
        response.body.message.contains("Custom aspect type 'undefinedCaType' is not defined for control implementations")

        where:
        elementType << ElementType.RISK_AFFECTED_TYPES
    }

    def "cannot add custom aspects to CI when control is not associated with the domain"() {
        given: "a control only in domain 2 (not in domain 1)"
        def controlInDomain2Id = post("/domains/$domain2Id/controls", [
            name: "Control in domain 2 only",
            subType: "ComplCtl",
            status: "NEW",
            owner: [targetUri: "http://localhost/units/$unitId"],
        ]).body.resourceId

        and: "an asset in domain 2 with a CI for that control (and CI custom aspects in domain 2)"
        def assetId = post("/domains/$domain2Id/assets", [
            name: "asset",
            subType: "A",
            status: "NEW",
            owner: [targetUri: "http://localhost/units/$unitId"],
            controlImplementations: [
                [
                    control: [targetUri: "/controls/$controlInDomain2Id"],
                    customAspects: [
                        otherCa: [reviewDate: "2026-01-01"],
                    ],
                ],
            ],
        ]).body.resourceId

        and: "associate the same asset with domain 1 (control remains only in domain 2)"
        post("/domains/$domainId/assets/$assetId", [
            subType: "A",
            status: "NEW",
        ], 200)

        when: "adding CI custom aspects in domain 1 for the control that is only in domain 2"
        def getResponse = get("/domains/$domainId/assets/$assetId")
        def body = getResponse.body
        def ciForControl2 = body.controlImplementations.find {
            it.control.targetUri?.endsWith("/$controlInDomain2Id") || it.control.id == controlInDomain2Id
        }
        ciForControl2.customAspects = [
            ciDetails: [
                implementationNotes: "notes in domain 1",
                priority: "high",
            ],
        ]
        def response = put("/domains/$domainId/assets/$assetId", body, getResponse.getETag(), 422)

        then: "the request is rejected because the control is not associated with domain 1"
        response.body.message.contains("control is not associated with it")
    }

    def "different custom aspects per domain for same CI on #elementType.singularTerm"() {
        given: "a control in both domains"
        def controlBothId = post("/domains/$domainId/controls", [
            name: "Control in both domains",
            subType: "ComplCtl",
            status: "NEW",
            owner: [targetUri: "http://localhost/units/$unitId"],
        ]).body.resourceId
        post("/domains/$domain2Id/controls/$controlBothId", [
            subType: "ComplCtl",
            status: "NEW",
        ], 200)

        and: "an element in both domains with the CI"
        def elementId = post("/domains/$domainId/$elementType.pluralTerm", [
            name: "element in both domains",
            subType: "A",
            status: "NEW",
            owner: [targetUri: "http://localhost/units/$unitId"],
            controlImplementations: [
                [control: [targetUri: "/controls/$controlBothId"]],
            ],
        ]).body.resourceId
        post("/domains/$domain2Id/$elementType.pluralTerm/$elementId", [
            subType: "A",
            status: "NEW",
        ], 200)

        when: "setting domain 1 custom aspects on the CI"
        get("/domains/$domainId/$elementType.pluralTerm/$elementId").with{
            body.controlImplementations[0].customAspects = [
                ciDetails: [
                    implementationNotes: "notes in domain 1",
                    priority: "high",
                ],
            ]
            put(body._self, body, getETag())
        }

        and: "setting domain 2 custom aspects on the same CI"
        get("/domains/$domain2Id/$elementType.pluralTerm/$elementId").with{
            body.controlImplementations[0].customAspects = [
                otherCa: [
                    reviewDate: "2026-12-31",
                ],
            ]
            put(body._self, body, getETag())
        }

        then: "each domain returns only its own CI custom aspects"
        with(get("/domains/$domainId/$elementType.pluralTerm/$elementId").body.controlImplementations[0].customAspects) {
            ciDetails.implementationNotes == "notes in domain 1"
            ciDetails.priority == "high"
            otherCa == null
        }
        with(get("/domains/$domain2Id/$elementType.pluralTerm/$elementId").body.controlImplementations[0].customAspects) {
            otherCa.reviewDate == "2026-12-31"
            ciDetails == null
        }

        where:
        elementType << ElementType.RISK_AFFECTED_TYPES
    }

    def "export and import unit preserves CI custom aspect values for #elementType.singularTerm"() {
        given: "an element with a CI that has custom aspects"
        def elementId = post("/domains/$domainId/$elementType.pluralTerm", [
            name: "element for export test",
            subType: "A",
            status: "NEW",
            owner: [targetUri: "http://localhost/units/$unitId"],
            controlImplementations: [
                [
                    control: [targetUri: "/controls/$controlId"],
                    customAspects: [
                        ciDetails: [
                            implementationNotes: "Exported notes",
                            priority: "imported",
                        ],
                    ],
                ],
            ],
        ]).body.resourceId

        when: "exporting the unit"
        def unitExport = get("/units/$unitId/export").body

        then: "the export contains the CI custom aspects"
        def elementInExport = unitExport.elements.find { it.id == elementId }
        elementInExport != null
        elementInExport.controlImplementations.size() == 1
        with(elementInExport.controlImplementations[0].domains[domainId].customAspects) {
            ciDetails != null
            ciDetails.implementationNotes == "Exported notes"
            ciDetails.priority == "imported"
        }

        when: "importing the unit (new unit)"
        def importResult = post("/units/import", unitExport, 201)
        def newUnitId = importResult.body.resourceId

        then: "the imported element has the same CI custom aspect values"
        def elementsInNewUnit = get("/domains/$domainId/$elementType.pluralTerm?unit=$newUnitId").body.items
        elementsInNewUnit.size() == 1
        with(elementsInNewUnit.first()) {
            name == "element for export test"
            with(controlImplementations.first().customAspects) {
                ciDetails != null
                ciDetails.implementationNotes == "Exported notes"
                ciDetails.priority == "imported"
            }
        }

        where:
        elementType << ElementType.RISK_AFFECTED_TYPES
    }
}
