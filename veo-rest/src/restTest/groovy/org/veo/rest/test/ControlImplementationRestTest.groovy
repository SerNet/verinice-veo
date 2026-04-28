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

import static org.veo.rest.CompactJsonHttpMessageConverter.MEDIA_TYPE_JSON_COMPACT

import org.veo.core.entity.ElementType

import spock.lang.Issue

class ControlImplementationRestTest extends VeoRestTest {
    private String domainId
    private String domain2Id
    private String unitId
    private String rootControl1Id
    private String rootControl2Id
    private String rootControl3Id
    private String subControl1Id
    private String subControl2Id
    private String subControl3Id
    private String subControl4Id
    private String person1Id
    private String person2Id

    def setup() {
        domainId = post("/content-creation/domains", [
            name: "CI/RI test domain ${UUID.randomUUID()}",
            authority: "JJ",
        ], 201, UserType.CONTENT_CREATOR).body.resourceId
        domain2Id = post("/content-creation/domains", [
            name: "CI/RI test domain ${UUID.randomUUID()}",
            authority: "JJ",
        ], 201, UserType.CONTENT_CREATOR).body.resourceId
        put("/content-creation/domains/$domainId/element-type-definitions/control", [
            subTypes: [
                ComplCtl: [statuses: ["NEW"]],
                OtherComplCtl: [statuses: ["NEW"]],
                MitiCtl: [statuses: ["NEW"]],
            ],
            customAspects: [
                oneCa: [
                    attributeDefinitions: [
                        someAttr: [type: "integer"]
                    ]
                ]
            ]
        ], null, 204, UserType.CONTENT_CREATOR)
        put("/content-creation/domains/$domain2Id/element-type-definitions/control", [
            subTypes: [
                ComplCtl: [statuses: ["NEW"]],
                MitiCtl: [statuses: ["NEW"]],
            ],
            customAspects: [
                oneCa: [
                    attributeDefinitions: [
                        someAttr: [type: "integer"]
                    ]
                ]
            ]
        ], null, 204, UserType.CONTENT_CREATOR)
        put("/content-creation/domains/$domainId/control-implementation-configuration", [
            complianceControlSubTypes: ["ComplCtl", "OtherComplCtl"],
            mitigationControlSubType: "MitiCtl"
        ], null, 204, UserType.CONTENT_CREATOR)

        put("/content-creation/domains/$domainId/element-type-definitions/process", [
            subTypes: [
                A: [statuses: ["NEW"]]
            ]
        ], null, 204, UserType.CONTENT_CREATOR)
        put("/content-creation/domains/$domainId/element-type-definitions/scope", [
            subTypes: [
                A: [statuses: ["NEW"]]
            ]
        ], null, 204, UserType.CONTENT_CREATOR)
        put("/content-creation/domains/$domainId/element-type-definitions/asset", [
            subTypes: [
                A: [statuses: ["NEW"]]
            ]
        ], null, 204, UserType.CONTENT_CREATOR)
        put("/content-creation/domains/$domainId/element-type-definitions/person", [
            subTypes: [
                A: [statuses: ["NEW"]]
            ]
        ], null, 204, UserType.CONTENT_CREATOR)
        put("/content-creation/domains/$domainId/element-type-definitions/scenario", [
            subTypes: [
                A: [statuses: ["NEW"]]
            ]
        ], null, 204, UserType.CONTENT_CREATOR)
        unitId = postNewUnit("U1", [
            domainId,
            domain2Id,
            testDomainId
        ]).resourceId

        subControl1Id = post("/domains/$domainId/controls", [
            name: "sub control 1",
            owner: [targetUri: "http://localhost/units/$unitId"],
            customAspects: [oneCa: [someAttr: 3]],
            subType: "MitiCtl",
            status: "NEW",
        ]).body.resourceId
        subControl2Id = post("/domains/$domain2Id/controls", [
            name: "sub control 2",
            subType: "MitiCtl",
            status: "NEW",
            owner: [targetUri: "http://localhost/units/$unitId"]
        ]).body.resourceId
        rootControl1Id = post("/domains/$domainId/controls", [
            name: "root control 1",
            subType: "MitiCtl",
            status: "NEW",
            owner: [targetUri: "http://localhost/units/$unitId"],
            parts: [
                [targetUri: "http://localhost/controls/$subControl1Id"],
                [targetUri: "http://localhost/controls/$subControl2Id"],
            ],
        ]).body.resourceId
        subControl3Id = post("/domains/$domain2Id/controls", [
            name: "sub control 3",
            subType: "ComplCtl",
            status: "NEW",
            owner: [targetUri: "http://localhost/units/$unitId"],
        ]).body.resourceId
        rootControl2Id = post("/domains/$domainId/controls", [
            name: "root control 2",
            subType: "ComplCtl",
            status: "NEW",
            owner: [targetUri: "http://localhost/units/$unitId"],
            parts: [
                [targetUri: "http://localhost/controls/$subControl3Id"]
            ],
        ]).body.resourceId
        subControl4Id = post("/domains/$domainId/controls", [
            name: "sub control 4",
            subType: "OtherComplCtl",
            status: "NEW",
            owner: [targetUri: "http://localhost/units/$unitId"],
        ]).body.resourceId
        rootControl3Id = post("/domains/$domainId/controls", [
            name: "root control 3",
            subType: "OtherComplCtl",
            status: "NEW",
            owner: [targetUri: "http://localhost/units/$unitId"],
            parts: [
                [targetUri: "http://localhost/controls/$subControl4Id"],
            ],
        ]).body.resourceId

        person1Id = post("/domains/$domainId/persons", [
            name: "person 1",
            subType: 'A',
            status: 'NEW',
            owner: [targetUri: "http://localhost/units/$unitId"],
        ]).body.resourceId
        person2Id = post("/domains/$domainId/persons", [
            name: "person 2",
            subType: 'A',
            status: 'NEW',
            owner: [targetUri: "http://localhost/units/$unitId"],
        ]).body.resourceId
    }

    def "CRUD CIs & RIs in domain for #elementType.singularTerm"() {
        when: "creating an element with two CIs"
        def elementId = post("/domains/$domainId/$elementType.pluralTerm", [
            name: "lame",
            owner: [targetUri: "http://localhost/units/$unitId"],
            subType: "A",
            status: "NEW",
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

        then: "RIs are filtered by domain"
        with(get("/domains/$domainId/$elementType.pluralTerm/$elementId/control-implementations/$rootControl1Id/requirement-implementations", 200).body) {
            items.size() == 1
            totalItemCount == 1
        }

        with(get("/domains/$domainId/$elementType.pluralTerm/$elementId/control-implementations/$rootControl2Id/requirement-implementations?controlCustomAspects=oneCa", 200).body) {
            items.isEmpty()
            totalItemCount == 0
        }

        and: "can be enriched by CA's"
        with(get("/domains/$domainId/$elementType.pluralTerm/$elementId/control-implementations/$rootControl1Id/requirement-implementations?controlCustomAspects=oneCa", 200).body) {
            items.first().control.customAspects.oneCa.someAttr == 3
            totalItemCount == 1
        }

        and: "querying invalid CAs returns 404"
        with(get("/domains/$domainId/$elementType.pluralTerm/$elementId/control-implementations/$rootControl1Id/requirement-implementations?controlCustomAspects=foobar", 404).body) {
            message == 'Invalid custom aspect ID(s): [foobar], available aspects: [oneCa]'
        }

        where:
        elementType << ElementType.RISK_AFFECTED_TYPES
    }

    def "CRUD CIs & RIs for #elementType.singularTerm"() {
        when: "creating and fetching an element with two CIs"
        def elementId = post("/domains/$domainId/$elementType.pluralTerm", [
            name: "lame",
            subType: 'A',
            status: 'NEW',
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
                // TODO #3052 shouldn't the RI for root control 1 be here, too?
                totalItemCount == 2
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
                // TODO #3052 shouldn't the RI for root control 2 be here, too?
                totalItemCount == 1
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
            body.implementationUntil = '1970-01-01'
            body.cost = 123
            body.lastRevisionDate = '2000-07-01'
            body.lastRevisionBy = [targetUri: "/persons/$person1Id"]
            body.nextRevisionDate = '2500-01-01'
            body.nextRevisionBy = [targetUri: "/persons/$person1Id"]
            put(body._self, body, getETag(), 204)
        }
        get("/$elementType.pluralTerm/$elementId/requirement-implementations/$subControl2Id").with {
            body.status = "YES"
            body.implementationStatement = "Done!"
            body.responsible = [targetUri: "/persons/$person2Id"]
            body.implementationUntil = '3000-01-01'
            body.implementationDate = '2999-12-31'
            put(body._self, body, getETag(), 204)
        }

        then: "changes have been applied"
        with(get("/$elementType.pluralTerm/$elementId/requirement-implementations/$rootControl1Id").body) {
            status == "PARTIAL"
            implementationStatement == "It's a start"
            responsible.displayName.endsWith("person 2")
            implementationUntil == '1970-01-01'
            cost == 123
            lastRevisionDate == '2000-07-01'
            lastRevisionBy.displayName.endsWith("person 1")
            nextRevisionDate == '2500-01-01'
            nextRevisionBy.displayName.endsWith("person 1")
        }
        with(get("/$elementType.pluralTerm/$elementId/requirement-implementations/$subControl2Id").body) {
            status == "YES"
            implementationStatement == "Done!"
            responsible.displayName.endsWith("person 2")
            implementationUntil == '3000-01-01'
            implementationDate == '2999-12-31'
        }

        and: "implementation status is reflected in CI"
        get("/$elementType.pluralTerm/$elementId").body
                .controlImplementations
                .find { it.control.displayName.endsWith("root control 1") }
                .implementationStatus == "PARTIAL"

        when: "removing one CI"
        get("/domains/$domainId/$elementType.pluralTerm/$elementId").with {
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

        when: "exporting the unit"
        def unitExport = get("/units/$unitId/export").body

        then:
        with(unitExport.elements.find { it.name == "lame" }) {
            requirementImplementations.size() == 4
            with(requirementImplementations.find { it.control.name == "root control 1" }) {
                status == "PARTIAL"
                implementationStatement == "It's a start"
                responsible.name == "person 2"
            }
            with(requirementImplementations.find { it.control.name == "sub control 2" }) {
                status == "YES"
                implementationStatement == "Done!"
                responsible.name == "person 2"
            }
            with(requirementImplementations.find { it.control.name == "root control 2" }) {
                status == "UNKNOWN"
            }
            with(requirementImplementations.find { it.control.name == "sub control 3" }) {
                status == "UNKNOWN"
            }
        }

        where:
        elementType << ElementType.RISK_AFFECTED_TYPES
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
                [
                    control: [targetUri: "/controls/$rootControl3Id"],
                    description: "I say so."
                ],
            ]
        ]).body.resourceId
        def retrievedElement = get("/domains/$domainId/$elementType.pluralTerm/$elementId").body

        then: "CIs for both controls are present"
        retrievedElement.controlImplementations.size() == 3
        with(retrievedElement.controlImplementations.find { it.control.displayName.endsWith("root control 1") }) {
            implementationStatus == "UNKNOWN"
            description == "I have my reasons"
            control.targetInDomainUri.endsWith("/domains/$owner.domainId/controls/$owner.rootControl1Id")
        }
        with(retrievedElement.controlImplementations.find { it.control.displayName.endsWith("root control 2") }) {
            implementationStatus == "UNKNOWN"
            responsible.displayName.endsWith("person 1")
            control.targetInDomainUri.endsWith("/domains/$owner.domainId/controls/$owner.rootControl2Id")
        }
        with(retrievedElement.controlImplementations.find { it.control.displayName.endsWith("root control 3") }) {
            implementationStatus == "UNKNOWN"
            control.targetInDomainUri.endsWith("/domains/$owner.domainId/controls/$owner.rootControl3Id")
            description == "I say so."
        }

        and: "the RIs are not part of the domain-specific element representation"
        retrievedElement.requirementImplementations == null

        and:
        with(get("/domains/$domainId/$elementType.pluralTerm/$elementId/control-implementations?purpose=MITIGATION", 200).body) {
            items.size() == 1
            items[0].control.subType == "MitiCtl"
        }

        and:
        with(get("/domains/$domainId/$elementType.pluralTerm/$elementId/control-implementations?purpose=COMPLIANCE", 200).body) {
            items.size() == 2
            items.control*.subType ==~ ["ComplCtl", "OtherComplCtl"]
        }

        and:
        with(get("/domains/$domainId/$elementType.pluralTerm/$elementId/control-implementations", 200).body) {
            items.size() == 3
            items.control*.subType ==~ [
                "ComplCtl",
                "MitiCtl",
                "OtherComplCtl"
            ]
        }

        when: "the configuration is deleted"
        put("/content-creation/domains/$domainId/control-implementation-configuration", [:], null, 204, UserType.CONTENT_CREATOR)

        then: "filter for mitigation is not allowed"
        with(get("/domains/$domainId/$elementType.pluralTerm/$elementId/control-implementations?purpose=MITIGATION", 422).body) {
            message == "No mitigation control sub type(s) configured in domain."
        }

        and: "filter for compliance is not allowed"
        with(get("/domains/$domainId/$elementType.pluralTerm/$elementId/control-implementations?purpose=COMPLIANCE", 422).body) {
            message == "No compliance control sub type(s) configured in domain."
        }

        and:
        with(get("/domains/$domainId/$elementType.pluralTerm/$elementId/control-implementations", 200).body) {
            items.size() == 3
            items.control*.subType ==~ [
                "MitiCtl",
                "ComplCtl",
                "OtherComplCtl"
            ]
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
            size() == 2
            it*.description ==~ [
                "I say so.",
                "I've made changes"
            ]
        }

        where:
        elementType << ElementType.RISK_AFFECTED_TYPES
    }

    def "CIs are maintained for mitigations on #elementType.pluralTerm"() {
        given:
        defineSubTypeAndStatus(elementType)
        def elementId = post("/domains/$domainId/$elementType.pluralTerm", [
            name: "risk-afficer",
            subType: "A",
            status: "living",
            owner: [targetUri: "http://localhost/units/$unitId"],
            controlImplementations: [
                [control: [targetUri: "/controls/$rootControl1Id"]]
            ]
        ]).body.resourceId
        def scenario1Id = post("/domains/$domainId/scenarios", [
            name: "scn",
            subType: 'A',
            status: 'NEW',
            owner: [targetUri: "http://localhost/units/$unitId"]
        ]).body.resourceId
        def scenario2Id = post("/domains/$domainId/scenarios", [
            name: "scn 2",
            subType: 'A',
            status: 'NEW',
            owner: [targetUri: "http://localhost/units/$unitId"]
        ]).body.resourceId

        when: "creating a mitigated risk"
        post("/$elementType.pluralTerm/$elementId/risks", [
            scenario: [targetUri: "/scenarios/$scenario1Id"],
            mitigation: [targetUri: "/controls/$rootControl2Id"],
            domains: [
                (domainId): [reference: [targetUri: "/domains/$domainId"]]
            ]
        ])

        then: "a CI has been added"
        get("/domains/$domainId/$elementType.pluralTerm/$elementId/control-implementations").body.items*.control*.name ==~ [
            "root control 1",
            "root control 2"
        ]
        get("/$elementType.pluralTerm/$elementId/requirement-implementations/$subControl1Id", 200)
        get("/$elementType.pluralTerm/$elementId/requirement-implementations/$subControl2Id", 200)
        get("/$elementType.pluralTerm/$elementId/requirement-implementations/$subControl3Id", 200)

        expect: "that the mitigation CI cannot be removed manually"
        with(get("/domains/$domainId/$elementType.pluralTerm/$elementId")) {
            body.controlImplementations.removeIf { it.control.name == 'root control 2' }
            put(body._self, body, getETag(), 422).body.message == "Control 'root control 2' ($rootControl2Id) cannot be disassociated, because it mitigates a risk for scenario 'scn' ($scenario1Id)."
        }

        when: "changing the mitigating control"
        def elementETagBeforeMitigationChange = get("/domains/$domainId/$elementType.pluralTerm/$elementId").getETag()
        get("/$elementType.pluralTerm/$elementId/risks/$scenario1Id").with {
            body.mitigation.targetUri = "/controls/$rootControl3Id"
            put(body._self, body, getETag(), 200)
        }

        then: "the CI has been replaced"
        get("/domains/$domainId/$elementType.pluralTerm/$elementId/control-implementations").body.items*.control*.name ==~ [
            "root control 1",
            "root control 3"
        ]
        get("/$elementType.pluralTerm/$elementId/requirement-implementations/$subControl3Id", 404)
        get("/$elementType.pluralTerm/$elementId/requirement-implementations/$subControl4Id", 200)

        and:
        get("/domains/$domainId/$elementType.pluralTerm/$elementId").getETag() != elementETagBeforeMitigationChange

        when: "adding another risk with a different mitigating control"
        post("/$elementType.pluralTerm/$elementId/risks", [
            scenario: [targetUri: "/scenarios/$scenario2Id"],
            mitigation: [targetUri: "/controls/$rootControl2Id"],
            domains: [
                (domainId): [reference: [targetUri: "/domains/$domainId"]]
            ]
        ])

        then: "a CI has been added"
        get("/domains/$domainId/$elementType.pluralTerm/$elementId/control-implementations").body.items*.control*.name ==~ [
            "root control 1",
            "root control 2",
            "root control 3"
        ]
        get("/$elementType.pluralTerm/$elementId/requirement-implementations/$subControl3Id", 200)

        when: "changing the new risk to use the same control as the other risk"
        get("/$elementType.pluralTerm/$elementId/risks/$scenario2Id").with {
            body.mitigation = [targetUri: "/controls/$rootControl3Id"]
            put(body._self, body, getETag(), 200)
        }

        then: "the CI has been removed"
        get("/domains/$domainId/$elementType.pluralTerm/$elementId/control-implementations").body.items*.control*.name ==~ [
            "root control 1",
            "root control 3"
        ]
        get("/$elementType.pluralTerm/$elementId/requirement-implementations/$subControl3Id", 404)

        when: "removing the mitigation from the second risk"
        get("/$elementType.pluralTerm/$elementId/risks/$scenario2Id").with {
            body.mitigation = null
            put(body._self, body, getETag(), 200)
        }

        then: "CIs haven't changed"
        get("/domains/$domainId/$elementType.pluralTerm/$elementId/control-implementations").body.items*.control*.name ==~ [
            "root control 1",
            "root control 3"
        ]

        when: "removing the mitigation from the original risk as well"
        get("/$elementType.pluralTerm/$elementId/risks/$scenario1Id").with {
            body.mitigation = null
            put(body._self, body, getETag(), 200)
        }

        then: "the CI is gone"
        get("/domains/$domainId/$elementType.pluralTerm/$elementId/control-implementations").body.items*.control*.name == ["root control 1"]
        get("/$elementType.pluralTerm/$elementId/requirement-implementations/$subControl4Id", 404)

        when: "mitigating the risk yet again"
        get("/$elementType.pluralTerm/$elementId/risks/$scenario1Id").with {
            body.mitigation = [targetUri: "/controls/$rootControl2Id"]
            put(body._self, body, getETag(), 200)
        }

        then: "a CI has been added"
        get("/domains/$domainId/$elementType.pluralTerm/$elementId/control-implementations").body.items*.control*.name ==~ [
            "root control 1",
            "root control 2"
        ]
        get("/$elementType.pluralTerm/$elementId/requirement-implementations/$subControl3Id", 200)

        when: "deleting the risk"
        delete("/$elementType.pluralTerm/$elementId/risks/$scenario1Id")

        then: "the CI is gone again"
        get("/domains/$domainId/$elementType.pluralTerm/$elementId/control-implementations").body.items*.control*.name == ["root control 1"]
        get("/$elementType.pluralTerm/$elementId/requirement-implementations/$subControl1Id", 200)
        get("/$elementType.pluralTerm/$elementId/requirement-implementations/$subControl2Id", 200)
        get("/$elementType.pluralTerm/$elementId/requirement-implementations/$subControl3Id", 404)
        get("/$elementType.pluralTerm/$elementId/requirement-implementations/$subControl4Id", 404)

        where:
        elementType << ElementType.RISK_AFFECTED_TYPES
    }

    def "elements used by #elementType.singularTerm CIs & RIs can be deleted"() {
        when: "creating and fetching an element with one CIs and a responsible person"
        def personId = post("/domains/$domainId/persons", [
            name: "person will be removed",
            subType: 'A',
            status: 'NEW',
            owner: [targetUri: "http://localhost/units/$unitId"],
        ]).body.resourceId

        def elementId = post("/domains/$domainId/$elementType.pluralTerm", [
            name: "lame",
            subType: 'A',
            status: 'NEW',
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

        get("/$elementType.pluralTerm/$elementId/requirement-implementations/$rootControl1Id").body.responsible == null

        when: "deleting a sub control"
        delete("/controls/$subControl1Id")

        then: "its RI is gone"
        get("/$elementType.pluralTerm/$elementId/requirement-implementations/$subControl1Id", 404)

        when: "deleting the root control"
        delete("/controls/$rootControl1Id")

        then: "its CI and all of the RIs are gone"
        get("/$elementType.pluralTerm/$elementId").body.controlImplementations == []
        get("/$elementType.pluralTerm/$elementId/requirement-implementations/$rootControl1Id", 404)
        get("/$elementType.pluralTerm/$elementId/requirement-implementations/$subControl2Id", 404)

        where:
        elementType << ElementType.RISK_AFFECTED_TYPES
    }

    @Issue("#2815")
    // Test removing the person from this graph:
    //```mermaid
    //graph TD
    //    P1[PER-1]:::person
    //    A1[AST-1]:::asset
    //    A2[AST-2]:::asset
    //    INF.11:::control
    //    SYS.4.4:::control
    //    A1 --> P1
    //    A1 --> A2
    //    A2 --> P1
    //    A1 -- CI --> SYS.4.4
    //    A2 -- CI --> INF.11
    //```
    def "delete person when assets own CIs and also link to person"() {
        given: "an asset that is linked with a person and has a CI"
        post("/domains/$testDomainId/persons/$person1Id", [
            subType: "MasterOfDisaster",
            status: "WATCHING_DISASTER_MOVIES",
        ], 200)

        def controlInf11Id = post("/domains/$testDomainId/controls", [
            name: "INF.11",
            subType: 'TOM',
            status: 'NEW',
            owner: [targetUri: "http://localhost/units/$unitId"],
        ]).body.resourceId

        def controlSys44Id = post("/domains/$testDomainId/controls", [
            name: "SYS.4.4",
            subType: 'TOM',
            status: 'NEW',
            owner: [targetUri: "http://localhost/units/$unitId"],
        ]).body.resourceId

        def asset2Id = post("/domains/$testDomainId/assets", [
            name: "AST-2",
            owner: [targetUri: "http://localhost/units/$unitId"],
            subType: "Server",
            status: "RUNNING",
            riskValues: [
                riskyDef: [potentialImpacts: [:]]
            ],
            links: [
                admin: [
                    [
                        target: [targetUri: "/persons/$person1Id"]
                    ]
                ]
            ],
            controlImplementations: [
                [
                    control: [targetUri: "/controls/$controlInf11Id"]
                ]
            ]
        ]).body.resourceId

        def asset1Id = post("/domains/$testDomainId/assets", [
            name: "AST-1",
            owner: [targetUri: "http://localhost/units/$unitId"],
            subType: "Server",
            status: "RUNNING",
            riskValues: [
                riskyDef: [potentialImpacts: [:]]
            ],
            links: [
                admin: [
                    [
                        target: [targetUri: "/persons/$person1Id"]
                    ]
                ],
                requires: [
                    [
                        target: [targetUri: "/assets/$asset2Id"]
                    ]
                ]
            ],
            controlImplementations: [
                [
                    control: [targetUri: "/controls/$controlSys44Id"]
                ]
            ]
        ]).body.resourceId

        when: "delete the person"
        delete("/persons/$person1Id")

        then: "the person was removed"
        get("/domains/$testDomainId/persons/$person1Id", 404)

        and: "the assets can be retrieved without an error"
        with(get("/domains/$testDomainId/assets/$asset1Id").body) {
            name == "AST-1"
        }
        with(get("/domains/$testDomainId/assets/$asset2Id").body) {
            name == "AST-2"
        }
    }

    def "concurrent requirement implementation changes on #elementType.singularTerm are detected"() {
        given:
        def elementId = post("/domains/$domainId/$elementType.pluralTerm", [
            name: "risk aficionado",
            subType: 'A',
            status: 'NEW',
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
        elementType << ElementType.RISK_AFFECTED_TYPES
    }

    def "origin of a requirement implementation on #elementType.singularTerm cannot be changed"() {
        given:
        def elementId = post("/domains/$domainId/$elementType.pluralTerm", [
            name: "protagonist",
            subType: 'A',
            status: 'NEW',
            owner: [targetUri: "http://localhost/units/$unitId"],
            controlImplementations: [
                [
                    control: [targetUri: "/controls/$subControl2Id"]
                ]
            ]
        ]).body.resourceId
        def otherElementId = post("/domains/$domainId/assets", [
            name: "antagonist",
            subType: 'A',
            status: 'NEW',
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
        elementType << ElementType.RISK_AFFECTED_TYPES
    }

    def "control of a requirement implementation on #elementType.singularTerm cannot be changed"() {
        given:
        def elementId = post("/domains/$domainId/$elementType.pluralTerm", [
            name: "risk aficionado",
            subType: 'A',
            status: 'NEW',
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
        elementType << ElementType.RISK_AFFECTED_TYPES
    }

    def "cannot create control implementation for control from another unit"() {
        given: "another unit"
        def otherUnitId = postNewUnit().resourceId

        expect: "that a control implementation cannot be created there referencing a control in the main unit"
        post("/domains/$domainId/$elementType.pluralTerm", [
            name: "risk influencer",
            subType: 'A',
            status: 'NEW',
            owner: [targetUri: "http://localhost/units/$otherUnitId"],
            controlImplementations: [
                [
                    control: [targetUri: "/controls/$rootControl1Id"]
                ]
            ]
        ], 422).body.message == "Elements in other units must not be referenced"

        where:
        elementType << ElementType.RISK_AFFECTED_TYPES
    }

    def "cannot assign person from another unit as responsible for control implementation"() {
        given: "a person in another unit"
        def otherUnitId = postNewUnit('Other unit', [domainId]).resourceId
        def personId = post("/domains/$domainId/persons", [
            name: "person in other unit",
            subType: 'A',
            status: 'NEW',
            owner: [targetUri: "/units/$otherUnitId"]
        ]).body.resourceId

        expect: "that it cannot be assigned as a responsible person in this unit"
        post("/domains/$domainId/$elementType.pluralTerm", [
            name: "risk influencer",
            subType: 'A',
            status: 'NEW',
            owner: [targetUri: "http://localhost/units/$unitId"],
            controlImplementations: [
                [
                    control: [targetUri: "/controls/$rootControl1Id"],
                    responsible: [targetUri: "/persons/$personId"],
                ]
            ]
        ], 422).body.message == "Elements in other units must not be referenced"

        where:
        elementType << ElementType.RISK_AFFECTED_TYPES
    }

    def "cannot assign person from another unit as responsible for requirement implementation"() {
        given: "a control implementation"
        def elementId = post("/domains/$domainId/$elementType.pluralTerm", [
            name: "risk influencer",
            subType: 'A',
            status: 'NEW',
            owner: [targetUri: "http://localhost/units/$unitId"],
            controlImplementations: [
                [
                    control: [targetUri: "/controls/$rootControl1Id"]
                ]
            ]
        ]).body.resourceId

        and: "a person in another unit"
        def otherUnitId = postNewUnit('Other unit', [domainId]).resourceId
        def personId = post("/domains/$domainId/persons", [
            name: "person in other unit",
            subType: 'A',
            status: 'NEW',
            owner: [targetUri: "/units/$otherUnitId"]
        ]).body.resourceId

        expect: "that the person cannot be added as an RI responsible"
        get("/$elementType.pluralTerm/$elementId/requirement-implementations/$rootControl1Id").with{
            body.responsible = [targetUri: "/persons/$personId"]
            put(body._self, body, getETag(), 422)
        }.body.message == "Elements in other units must not be referenced"

        where:
        elementType << ElementType.RISK_AFFECTED_TYPES
    }

    def "can use linked person as responsible for RI"() {
        given: "an asset that is linked with a person and has a CI"
        post("/domains/$testDomainId/persons/$person1Id", [
            subType: "MasterOfDisaster",
            status: "WATCHING_DISASTER_MOVIES",
        ], 200)
        def assetId = post("/domains/$testDomainId/assets", [
            name: "silly server",
            owner: [targetUri: "http://localhost/units/$unitId"],
            subType: "Server",
            status: "RUNNING",
            links: [
                admin: [
                    [
                        target: [targetUri: "/persons/$person1Id"]
                    ]
                ]
            ],
            controlImplementations: [
                [
                    control: [targetUri: "/controls/$rootControl1Id"]
                ]
            ]
        ]).body.resourceId

        when: "using the same person as responsible for the root control RI"
        get("/assets/$assetId/requirement-implementations/$rootControl1Id").with{
            body.responsible = [targetUri: "/persons/$person1Id"]
            body.status = "YES"
            put(body._self, body, getETag(), 204)
        }

        then: "the asset can be retrieved without an error"
        with(get("/domains/$testDomainId/assets/$assetId").body) {
            controlImplementations[0].implementationStatus == "YES"
        }
        with(get("/domains/$testDomainId/assets?unit=$unitId").body) {
            items.find { it.id == assetId }.controlImplementations[0].implementationStatus == "YES"
        }
        with(get("/assets?unit=$unitId").body) {
            items.find { it.id == assetId }.controlImplementations[0].implementationStatus == "YES"
        }
        with(get("/assets/$assetId").body) {
            controlImplementations[0].implementationStatus == "YES"
        }
        with(get("/units/$unitId/export").body) {
            elements.find { it.id == assetId }.controlImplementations[0].implementationStatus == "YES"
        }
    }

    def "missing resources are handled for #elementType.pluralTerm"() {
        given:
        var randomUuid = UUID.randomUUID().toString()
        def elementId = post("/domains/$domainId/$elementType.pluralTerm", [
            name: "risk affe",
            subType: 'A',
            status: 'NEW',
            owner: [targetUri: "http://localhost/units/$unitId"],
            controlImplementations: [
                [
                    control: [targetUri: "/controls/$subControl2Id"]
                ]
            ]
        ]).body.resourceId

        expect:
        get("/$elementType.pluralTerm/$randomUuid/control-implementations/$subControl2Id/requirement-implementations", 404)
                .body.message == "$elementType.singularTerm $randomUuid not found"
        get("/$elementType.pluralTerm/$elementId/control-implementations/$randomUuid/requirement-implementations", 404)
                .body.message == "control $randomUuid not found"
        get("/$elementType.pluralTerm/$elementId/control-implementations/$subControl3Id/requirement-implementations", 404)
                .body.message == "$elementType.singularTerm $elementId does not implement control $subControl3Id"

        and:
        get("/$elementType.pluralTerm/$elementId/requirement-implementations/$randomUuid", 404)
                .body.message == "control $randomUuid not found"
        get("/$elementType.pluralTerm/$elementId/requirement-implementations/$subControl3Id", 404)
                .body.message == "$elementType.singularTerm $elementId contains no requirement implementation for control $subControl3Id"

        and:
        put("/$elementType.pluralTerm/$elementId/requirement-implementations/$randomUuid", [
            origin: [targetUri: "/$elementType.pluralTerm/$elementId"],
            control: [targetUri: "/controls/$subControl2Id"],
            status: "YES",
            origination: "SYSTEM_SPECIFIC",
        ], "", 404)
        .body.message == "control $randomUuid not found"
        put("/$elementType.pluralTerm/$elementId/requirement-implementations/$subControl3Id", [
            origin: [targetUri: "/$elementType.pluralTerm/$elementId"],
            control: [targetUri: "/controls/$subControl2Id"],
            status: "YES",
            origination: "SYSTEM_SPECIFIC",
        ], "", 404)
        .body.message == "$elementType.singularTerm $elementId contains no requirement implementation for control $subControl3Id"

        where:
        elementType << ElementType.RISK_AFFECTED_TYPES
    }

    def "CIs and RIs for #elementType.pluralTerm can be fetched in compact representation"() {
        given:
        def elementId = post("/domains/$domainId/$elementType.pluralTerm", [
            name: "affectionate",
            subType: 'A',
            status: 'NEW',
            owner: [targetUri: "http://localhost/units/$unitId"],
            controlImplementations: [
                [
                    control: [targetUri: "/controls/$subControl2Id"]
                ]
            ]
        ]).body.resourceId

        expect:
        with(get("/$elementType.pluralTerm/$elementId", 200, UserType.DEFAULT, MEDIA_TYPE_JSON_COMPACT).body.controlImplementations[0]) {
            control.id == subControl2Id
            _requirementImplementations == null
            implementationStatus == null
        }

        with(get("/$elementType.pluralTerm/$elementId/requirement-implementations/$subControl2Id", 200, UserType.DEFAULT, MEDIA_TYPE_JSON_COMPACT).body) {
            control.id == subControl2Id
            status == "UNKNOWN"
            _self == null
        }

        where:
        elementType << ElementType.RISK_AFFECTED_TYPES
    }

    def "RIs are synced with control parts"() {
        given: "a super control for root control 3"
        def rootControl3SuperId = post("/domains/$domainId/controls", [
            name: "root control 3 superior",
            subType: "MitiCtl",
            status: "NEW",
            owner: [targetUri: "http://localhost/units/$unitId"],
            parts: [
                [targetUri: "http://localhost/controls/$rootControl3Id"],
            ],
        ]).body.resourceId

        and: "an asset with CIs for all the root controls"
        def assetId = post("/domains/$domainId/assets", [
            name: "elem",
            subType: 'A',
            status: 'NEW',
            owner: [targetUri: "http://localhost/units/$unitId"],
            controlImplementations: [
                [
                    control: [targetUri: "/controls/$rootControl1Id"]
                ],
                [
                    control: [targetUri: "/controls/$rootControl2Id"]
                ],
                [
                    control: [targetUri: "/controls/$rootControl3Id"]
                ],
                [
                    control: [targetUri: "/controls/$rootControl3SuperId"]
                ],
            ]
        ]).body.resourceId

        expect:
        get("/assets/$assetId/control-implementations/$rootControl1Id/requirement-implementations").body.items*.control*.name ==~ [
            'sub control 1',
            'sub control 2'
        ]
        get("/assets/$assetId/control-implementations/$rootControl2Id/requirement-implementations").body.items*.control*.name ==~ ['sub control 3']
        get("/assets/$assetId/control-implementations/$rootControl3Id/requirement-implementations").body.items*.control*.name ==~ ['sub control 4']
        get("/assets/$assetId/control-implementations/$rootControl3SuperId/requirement-implementations").body.items*.control*.name ==~ [
            'sub control 4',
        ]

        when: "adding sub controls to a root control"
        get("/domains/$domainId/controls/$rootControl3Id").with{
            body.parts.addAll([
                [targetUri: "/controls/$subControl2Id"],
                [targetUri: "/controls/$subControl3Id"],
            ])
            put(body._self, body, getETag())
        }

        then: "the RIs are synced"
        get("/assets/$assetId/control-implementations/$rootControl1Id/requirement-implementations").body.items*.control*.name ==~ [
            'sub control 1',
            'sub control 2'
        ]
        get("/assets/$assetId/control-implementations/$rootControl2Id/requirement-implementations").body.items*.control*.name ==~ ['sub control 3']
        get("/assets/$assetId/control-implementations/$rootControl3Id/requirement-implementations").body.items*.control*.name ==~ [
            'sub control 2',
            'sub control 3',
            'sub control 4'
        ]
        get("/assets/$assetId/control-implementations/$rootControl3SuperId/requirement-implementations").body.items*.control*.name ==~ [
            'sub control 2',
            'sub control 3',
            'sub control 4'
        ]

        when: "removing sub controls from a root control"
        get("/domains/$domainId/controls/$rootControl3Id").with{
            body.parts.removeIf{it.id == subControl2Id}
            body.parts.removeIf{it.id == subControl4Id}
            put(body._self, body, getETag())
        }

        then: "the RIs are synced"
        get("/assets/$assetId/control-implementations/$rootControl1Id/requirement-implementations").body.items*.control*.name ==~ [
            'sub control 1',
            'sub control 2'
        ]
        get("/assets/$assetId/control-implementations/$rootControl2Id/requirement-implementations").body.items*.control*.name ==~ ['sub control 3']
        get("/assets/$assetId/control-implementations/$rootControl3Id/requirement-implementations").body.items*.control*.name ==~ [
            'sub control 3',
        ]
        get("/assets/$assetId/control-implementations/$rootControl3SuperId/requirement-implementations").body.items*.control*.name ==~ [
            'sub control 3',
        ]

        when: "removing more parts"
        get("/domains/$domainId/controls/$rootControl3SuperId").with{
            body.parts = []
            put(body._self, body, getETag())
        }
        get("/domains/$domainId/controls/$rootControl3Id").with{
            body.parts = []
            put(body._self, body, getETag())
        }

        then: "the RIs are synced"
        get("/assets/$assetId/control-implementations/$rootControl1Id/requirement-implementations").body.items*.control*.name ==~ [
            'sub control 1',
            'sub control 2'
        ]
        get("/assets/$assetId/control-implementations/$rootControl2Id/requirement-implementations").body.items*.control*.name ==~ ['sub control 3']
        get("/assets/$assetId/control-implementations/$rootControl3Id/requirement-implementations").body.items*.control*.name ==~ [
            'root control 3',
        ]
        get("/assets/$assetId/control-implementations/$rootControl3SuperId/requirement-implementations").body.items*.control*.name ==~ [
            'root control 3 superior',
        ]
    }

    String defineSubTypeAndStatus(ElementType type) {
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
