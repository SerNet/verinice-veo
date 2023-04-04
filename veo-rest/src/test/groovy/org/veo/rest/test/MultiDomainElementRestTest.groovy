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
        domainIdA = post("/domains", [
            name: "multi domain element rest test domain A ${randomUUID()}",
            authority: "jj",
        ], 201, UserType.CONTENT_CREATOR).body.resourceId
        domainIdB = post("/domains", [
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
        get("/domians/$domainIdA/$type.pluralTerm/$elementId").with {
            def element = body
            element.customAspects.separateCa = [
                someAttr: "fooA"
            ]
            put(element._self, element, getETag())
        }

        then: "CA has been applied in domain A"
        with(get("/domians/$domainIdA/$type.pluralTerm/$elementId").body) {
            customAspects == [
                separateCa: [
                    someAttr: "fooA"
                ]
            ]
        }

        and: "CAs is absent in domain B"
        with(get("/domians/$domainIdB/$type.pluralTerm/$elementId").body) {
            customAspects == [:]
        }

        when: "using CA in domain B"
        get("/domians/$domainIdB/$type.pluralTerm/$elementId").with {
            def element = body
            element.customAspects.separateCa = [
                someAttr: 42
            ]
            put(element._self, element, getETag())
        }

        then: "CA has been applied in domain B"
        with(get("/domians/$domainIdB/$type.pluralTerm/$elementId").body) {
            customAspects == [
                separateCa: [
                    someAttr: 42
                ]
            ]
        }

        and: "CA in domain A is still the same"
        with(get("/domians/$domainIdA/$type.pluralTerm/$elementId").body) {
            customAspects == [
                separateCa: [
                    someAttr: "fooA"
                ]
            ]
        }

        where:
        type << EntityType.ELEMENT_TYPES
    }

    // TODO VEO-1874 expect element to be versioned independently in different domain contexts
    def "#type.singularTerm ETags are handled correctly across domains"() {
        given: "an element in two domains"
        putElementTypeDefinitions(type)
        // TODO VEO-1871 associate using new POST endpoint
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

        when: "fetching ETag on element root resource"
        final def initialETag = get("/$type.pluralTerm/$elementId").getETag()

        then: "it's set"
        initialETag =~ /".+"/

        and: "domain-specific ETags are identical"
        get("/domians/$domainIdA/$type.pluralTerm/$elementId").getETag() == initialETag
        get("/domians/$domainIdB/$type.pluralTerm/$elementId").getETag() == initialETag

        when: "updating element in one domain"
        def putETag = get("/domians/$domainIdA/$type.pluralTerm/$elementId").body.with {
            it.status = "OLD"
            put(it._self, it, initialETag, 200).getETag()
        }

        then: "the correct new ETag has been returned"
        putETag =~ /".+"/
        putETag == get("/$type.pluralTerm/$elementId").getETag()

        and: "ETags have changed on all resources"
        putETag != initialETag
        get("/domians/$domainIdA/$type.pluralTerm/$elementId").getETag() == putETag
        get("/domians/$domainIdB/$type.pluralTerm/$elementId").getETag() == putETag

        expect: "updating the element with old ETag to fail"
        get("/domians/$domainIdB/$type.pluralTerm/$elementId").body.with {
            it.status = "OFF"
            put(it._self, it, initialETag, 412)
        }.body.message == "The eTag does not match for the element with the ID $elementId"

        and: "updating the element with new ETag to succeed"
        get("/domians/$domainIdB/$type.pluralTerm/$elementId").body.with {
            it.status = "OFF"
            put(it._self, it, putETag, 200)
        }

        where:
        type << EntityType.ELEMENT_TYPES
    }

    private void putElementTypeDefinitions(EntityType type) {
        put("/domains/$domainIdA/element-type-definitions/$type.singularTerm", [
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
        put("/domains/$domainIdB/element-type-definitions/$type.singularTerm", [
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
