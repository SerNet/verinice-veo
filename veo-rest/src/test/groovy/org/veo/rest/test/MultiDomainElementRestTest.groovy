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

import static java.util.UUID.randomUUID

import org.veo.core.entity.EntityType

class MultiDomainElementRestTest extends VeoRestTest {

    private String domainIdA
    private String domainIdB
    private String unitId

    def setup() {
        unitId = post("/units", [
            name: "multi domain element rest test unit"
        ]).body.resourceId
        domainIdA = post("/content-creation/domains", [
            name: "multi domain element rest test domain A ${randomUUID()}",
            authority: "jj",
        ], 201, UserType.CONTENT_CREATOR).body.resourceId
        domainIdB = post("/content-creation/domains", [
            name: "multi domain element rest test domain B ${randomUUID()}",
            authority: "jj",
        ], 201, UserType.CONTENT_CREATOR).body.resourceId
        put("/units/$unitId", [
            name: "multi domain element rest test unit",
            domains: [
                [targetUri: "/domains/$domainIdA"],
                [targetUri: "/domains/$domainIdB"],
            ]
        ], get("/units/$unitId").getETag())
    }

    def "custom aspects for #type.pluralTerm are domain-specific"() {
        given:
        putElementTypeDefinitions(type)
        def elementId = post("/$type.pluralTerm", [
            name: "my little element",
            owner: [targetUri: "/units/$unitId"],
            domains: [
                (domainIdA): [
                    subType: "STA",
                    status: "NEW"
                ],
                (domainIdB): [
                    subType: "STB",
                    status: "ON"
                ]
            ]
        ]).body.resourceId

        when: "using CA in domain A"
        get("/domains/$domainIdA/$type.pluralTerm/$elementId").with {
            def element = body
            element.customAspects.separateCa = [
                someAttr: "fooA"
            ]
            put(element._self, element, getETag())
        }

        then: "CA has been applied in domain A"
        with(get("/domains/$domainIdA/$type.pluralTerm/$elementId").body) {
            customAspects == [
                separateCa: [
                    someAttr: "fooA"
                ]
            ]
        }

        and: "CAs is absent in domain B"
        with(get("/domains/$domainIdB/$type.pluralTerm/$elementId").body) {
            customAspects == [:]
        }

        when: "using CA in domain B"
        get("/domains/$domainIdB/$type.pluralTerm/$elementId").with {
            def element = body
            element.customAspects.separateCa = [
                someAttr: 42
            ]
            put(element._self, element, getETag())
        }

        then: "CA has been applied in domain B"
        with(get("/domains/$domainIdB/$type.pluralTerm/$elementId").body) {
            customAspects == [
                separateCa: [
                    someAttr: 42
                ]
            ]
        }

        and: "CA in domain A is still the same"
        with(get("/domains/$domainIdA/$type.pluralTerm/$elementId").body) {
            customAspects == [
                separateCa: [
                    someAttr: "fooA"
                ]
            ]
        }

        where:
        type << EntityType.ELEMENT_TYPES
    }

    def "identically defined custom aspects for #type.pluralTerm are synced across domains"() {
        given: "an element associated with both domains"
        putElementTypeDefinitions(type)
        def elementId = post("/$type.pluralTerm", [
            name: "my little element",
            owner: [targetUri: "/units/$unitId"],
            domains: [
                (domainIdA): [
                    subType: "STA",
                    status: "NEW"
                ],
                (domainIdB): [
                    subType: "STB",
                    status: "ON"
                ]
            ]
        ]).body.resourceId

        when: "adding CAs in domain A"
        get("/domains/$domainIdA/$type.pluralTerm/$elementId").with {
            def element = body
            element.customAspects = [
                identicalCa: [
                    someAttr: 7
                ],
                separateCa: [
                    someAttr: "magic"
                ]
            ]
            put(element._self, element, getETag())
        }

        then: "identical CA has been applied in both domains"
        with(get("/domains/$domainIdA/$type.pluralTerm/$elementId").body) {
            customAspects.identicalCa.someAttr == 7
            customAspects.separateCa.someAttr == "magic"
        }
        with(get("/domains/$domainIdB/$type.pluralTerm/$elementId").body) {
            customAspects.identicalCa.someAttr == 7
            customAspects.separateCa == null
        }

        when: "updating CAs in domain B"
        get("/domains/$domainIdB/$type.pluralTerm/$elementId").with {
            def element = body
            element.customAspects.identicalCa.someAttr = 8
            element.customAspects.separateCa = [
                someAttr: 9000
            ]
            put(element._self, element, getETag())
        }

        then: "identical CA has been applied in both domains"
        with(get("/domains/$domainIdA/$type.pluralTerm/$elementId").body) {
            customAspects.identicalCa.someAttr == 8
            customAspects.separateCa.someAttr == "magic"
        }
        with(get("/domains/$domainIdB/$type.pluralTerm/$elementId").body) {
            customAspects.identicalCa.someAttr == 8
            customAspects.separateCa.someAttr == 9000
        }

        when: "removing CA in domain B"
        get("/domains/$domainIdB/$type.pluralTerm/$elementId").with {
            def element = body
            element.customAspects.remove("identicalCa")
            put(element._self, element, getETag())
        }

        then: "it's missing from both domains"
        with(get("/domains/$domainIdB/$type.pluralTerm/$elementId").body) {
            customAspects.identicalCa == null
            customAspects.separateCa.someAttr == 9000
        }
        with(get("/domains/$domainIdA/$type.pluralTerm/$elementId").body) {
            customAspects.identicalCa == null
            customAspects.separateCa.someAttr == "magic"
        }

        where:
        type << EntityType.ELEMENT_TYPES
    }

    def "identically defined custom aspects are synced when associating a #type.singularTerm with a domain"() {
        given: "an element associated with domain A"
        putElementTypeDefinitions(type)
        def elementId = post("/domains/$domainIdA/$type.pluralTerm", [
            name: "my little element",
            owner: [targetUri: "/units/$unitId"],
            subType: "STA",
            status: "NEW"
        ]).body.resourceId

        when: "adding CAs in domain A"
        get("/domains/$domainIdA/$type.pluralTerm/$elementId").with {
            def element = body
            element.customAspects = [
                identicalCa: [
                    someAttr: 7
                ],
                separateCa: [
                    someAttr: "magic"
                ]
            ]
            put(element._self, element, getETag())
        }

        then: "CAs have been applied in domain A"
        with(get("/domains/$domainIdA/$type.pluralTerm/$elementId").body) {
            customAspects.identicalCa.someAttr == 7
            customAspects.separateCa.someAttr == "magic"
        }

        and: "not in domain B"
        get("/domains/$domainIdB/$type.pluralTerm/$elementId", 404)

        when: "associating element with domain B"
        post("/domains/$domainIdB/$type.pluralTerm/$elementId", [
            subType: "STB",
            status: "ON"
        ], 200)
        def elementInDomainB = get("/domains/$domainIdB/$type.pluralTerm/$elementId").body

        then: "identical CA has been applied in domain B"
        elementInDomainB.customAspects.identicalCa.someAttr == 7

        and: "different CA has not been applied"
        elementInDomainB.customAspects.separateCa == null

        where:
        type << EntityType.ELEMENT_TYPES
    }

    // TODO VEO-1874 expect element to be versioned independently in different domain contexts
    def "#type.singularTerm ETags are handled correctly across domains"() {
        given: "an unassociated element"
        putElementTypeDefinitions(type)
        def elementId = post("/$type.pluralTerm", [
            name: "my little element",
            owner: [targetUri: "/units/$unitId"],
        ]).body.resourceId

        when: "fetching ETag on element root resource"
        final def initialETag = get("/$type.pluralTerm/$elementId").getETag()

        then: "it's set"
        initialETag =~ /".+"/

        when: "assigning the element to both domains"
        post("/domains/$domainIdA/$type.pluralTerm/$elementId", [
            subType: "STA",
            status: "NEW"
        ], 200)
        post("/domains/$domainIdB/$type.pluralTerm/$elementId", [
            subType: "STB",
            status: "ON",
        ], 200)

        and: "fetching the ETag again"
        final def eTagAfterAssociating = get("/$type.pluralTerm/$elementId").getETag()

        then: "the ETag has changed"
        eTagAfterAssociating != initialETag

        and: "domain-specific ETags are identical"
        get("/domains/$domainIdA/$type.pluralTerm/$elementId").getETag() == eTagAfterAssociating
        get("/domains/$domainIdB/$type.pluralTerm/$elementId").getETag() == eTagAfterAssociating

        when: "updating element in one domain"
        def putETag = get("/domains/$domainIdA/$type.pluralTerm/$elementId").body.with {
            it.status = "OLD"
            put(it._self, it, eTagAfterAssociating, 200).getETag()
        }

        then: "the correct new ETag has been returned"
        putETag =~ /".+"/
        putETag == get("/$type.pluralTerm/$elementId").getETag()

        and: "ETags have changed on all resources"
        putETag != eTagAfterAssociating
        get("/domains/$domainIdA/$type.pluralTerm/$elementId").getETag() == putETag
        get("/domains/$domainIdB/$type.pluralTerm/$elementId").getETag() == putETag

        expect: "updating the element with old ETag to fail"
        get("/domains/$domainIdB/$type.pluralTerm/$elementId").body.with {
            it.status = "OFF"
            put(it._self, it, eTagAfterAssociating, 412)
        }.body.message == "The eTag does not match for the element with the ID $elementId"

        and: "updating the element with new ETag to succeed"
        get("/domains/$domainIdB/$type.pluralTerm/$elementId").body.with {
            it.status = "OFF"
            put(it._self, it, putETag, 200)
        }

        where:
        type << EntityType.ELEMENT_TYPES
    }

    def "#type.singularTerm domains must be a subset of unit's domains"() {
        given: "a unit with domain A"
        def unitUri = post("/units", [
            name: "domain A unit",
            domains: [
                [targetUri: "/domains/$domainIdA"]
            ]
        ]).location
        putElementTypeDefinitions(type)

        expect: "creating an element in the unit in domain B to fail"
        post("/domains/$domainIdB/$type.pluralTerm", [
            name: "illegal element",
            owner: [targetUri: unitUri],
            subType: "STB",
            status: "ON",
        ], 422).body.message == "Element can only be associated with its unit's domains"

        when: "adding domain B to the unit"
        get(unitUri).with{
            body.domains.add([targetUri: "/domains/$domainIdB"])
            put(unitUri, body, getETag())
        }

        then: "an element can be created in the unit in domain B"
        def elementId = post("/domains/$domainIdB/$type.pluralTerm", [
            name: "ok element",
            owner: [targetUri: unitUri],
            subType: "STB",
            status: "ON",
        ]).body.resourceId

        expect: "removing domain B from the unit to fail"
        get(unitUri).with{
            body.domains.removeIf { it.targetUri.contains(domainIdB) }
            with(put(unitUri, body, getETag(), 422)) {
                body.message == "Cannot remove domain $domainIdB from unit. 1 element(s) in the unit are associated with it, including: $type.singularTerm $elementId"
            }
        }

        when: "deleting the element"
        delete("/$type.pluralTerm/$elementId")

        then: "the domain can be removed from the unit"
        get(unitUri).with{
            body.domains.removeIf { it.targetUri.contains(domainIdB) }
            put(unitUri, body, getETag())
        }

        where:
        type << EntityType.ELEMENT_TYPES
    }

    def "cannot re-associate #type.singularTerm with domain"() {
        given: "an element associated with domain A"
        putElementTypeDefinitions(type)
        def elementId = post("/$type.pluralTerm", [
            name: "some element",
            owner: [targetUri: "/units/$unitId"]
        ]).body.resourceId
        post("/domains/$domainIdA/$type.pluralTerm/$elementId", [
            subType: "STA",
            status: "NEW",
        ], 200)

        expect: "re-assigning the element with domain A to fail"
        post("/domains/$domainIdA/$type.pluralTerm/$elementId", [
            subType: "STA",
            status: "OLD",
        ], 409).body.message == "$type.singularTerm $elementId is already associated with domain $domainIdA"

        where:
        type << EntityType.ELEMENT_TYPES
    }

    def "decisions are evaluated for #type.pluralTerm"() {
        given: "a decision in domain B that outputs whether someAttr is greater than 5"
        putElementTypeDefinitions(type)
        put("/content-creation/domains/$domainIdB/decisions/easyDecision", [
            name: [en: "Easy decision"],
            elementType: type.singularTerm,
            elementSubType: "STB",
            rules: [
                [
                    conditions: [
                        [
                            inputProvider: [
                                type: "customAspectAttributeValue",
                                customAspect: "separateCa",
                                attribute: "someAttr",
                            ],
                            inputMatcher: [
                                type: "greaterThan",
                                comparisonValue: 5
                            ]
                        ]
                    ],
                    output: true
                ]
            ],
            defaultResultValue: false
        ], null, 201, UserType.CONTENT_CREATOR)

        and: "a transient element where someAttr is 3"
        def element = [
            name: "decision test element",
            owner: [targetUri: "/units/$unitId"],
            subType: "STB",
            status: "ON",
            customAspects: [
                separateCa: [
                    someAttr: 3
                ]
            ]
        ]

        expect: "the decision result to be false"
        !post("/domains/$domainIdB/$type.pluralTerm/evaluation", element, 200).body.decisionResults.easyDecision.value

        when: "changing someAttr to 6"
        element.customAspects.separateCa.someAttr = 6

        then: "the decision result should be true"
        post("/domains/$domainIdB/$type.pluralTerm/evaluation", element, 200).body.decisionResults.easyDecision.value

        and: "the legacy evaluation endpoint also works"
        post("/$type.pluralTerm/evaluation?domain=$domainIdB", [
            name: "decision test element",
            owner: [targetUri: "/units/$unitId"],
            domains: [
                (domainIdB): [
                    subType: "STB",
                    status: "ON",
                ]
            ],
            customAspects: [
                separateCa: [
                    attributes: [
                        someAttr: 6
                    ]
                ]
            ]
        ], 200).body.decisionResults.easyDecision.value

        where:
        type << EntityType.ELEMENT_TYPES
    }

    private void putElementTypeDefinitions(EntityType type) {
        put("/content-creation/domains/$domainIdA/element-type-definitions/$type.singularTerm", [
            subTypes: [
                STA: [
                    statuses: ["NEW", "OLD"]
                ]
            ],
            customAspects: [
                identicalCa: [
                    attributeDefinitions: [
                        someAttr: [type: "integer"]
                    ]
                ],
                separateCa: [
                    attributeDefinitions: [
                        someAttr: [type: "text"]
                    ]
                ],
            ]
        ], "", 204)
        put("/content-creation/domains/$domainIdB/element-type-definitions/$type.singularTerm", [
            subTypes: [
                STB: [
                    statuses: ["ON", "OFF"]
                ]
            ],
            customAspects: [
                identicalCa: [
                    attributeDefinitions: [
                        someAttr: [type: "integer"]
                    ]
                ],
                separateCa: [
                    attributeDefinitions: [
                        someAttr: [type: "integer"]
                    ]
                ],
            ]
        ], "", 204)
    }
}
