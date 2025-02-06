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
import static org.veo.rest.test.UserType.CONTENT_CREATOR

import org.veo.core.entity.ElementType

class LinkingRestTest extends VeoRestTest {
    String domainId
    String unitId

    def setup() {
        domainId = post("/content-creation/domains", [
            name: randomUUID(),
            authority: "me",
        ], 201, CONTENT_CREATOR).body.resourceId
        unitId = post("/units", [
            name: "some unit",
            domains: [
                [targetUri: "/domains/$domainId"]
            ]
        ]).body.resourceId
    }

    def "asset can be linked with #targetType.pluralTerm"() {
        given: "a link definition between asset and target type"
        put("/content-creation/domains/$domainId/element-type-definitions/$targetType.singularTerm", [
            subTypes: [
                ST: [statuses: ["NEW"]]
            ]
        ], "", 204, CONTENT_CREATOR)
        put("/content-creation/domains/$domainId/element-type-definitions/asset", [
            subTypes: [
                ST: [statuses: ["NEW"]]
            ],
            links: [
                someLink: [
                    targetType: targetType.singularTerm,
                    targetSubType: "ST",
                ]
            ]
        ], "", 204, CONTENT_CREATOR)

        and: "a target element"
        def targetId = post("/domains/$domainId/$targetType.pluralTerm", [
            name: "target element",
            owner: [targetUri: "/units/$unitId"],
            subType: "ST",
            status: "NEW"
        ]).body.resourceId

        when: "creating an asset with a link to the target"
        def sourceId = post("/domains/$domainId/assets", [
            name: "source asset",
            owner: [targetUri: "/units/$unitId"],
            subType: "ST",
            status: "NEW",
            links: [
                someLink: [
                    [target: [targetInDomainUri: "/domains/$domainId/$targetType.pluralTerm/$targetId"]]
                ]
            ]
        ]).body.resourceId

        then: "the target has been set"
        with(get("/domains/$domainId/assets/$sourceId").body) {
            links.someLink[0].target.targetUri ==~ /.*\/$targetType.pluralTerm\/$targetId/
            links.someLink[0].target.targetInDomainUri ==~ /.*\/domains\/$owner.domainId\/$targetType.pluralTerm\/$targetId/
            links.someLink[0].target.displayName ==~ /.* target element/
        }

        where:
        targetType << ElementType.values()
    }

    def "links for #type.pluralTerm can be added using sub resource"() {
        given:
        put("/content-creation/domains/$domainId/element-type-definitions/$type.singularTerm", [
            subTypes: [
                STA: [
                    statuses: ["NEW"]
                ]
            ],
            links: [
                linkTypeA: [
                    targetType: type.singularTerm,
                    targetSubType: "STA",
                ],
                linkTypeB: [
                    targetType: type.singularTerm,
                    targetSubType: "STA",
                    attributeDefinitions: [
                        propOfTruth: [type: "boolean"]
                    ]
                ],
            ]
        ], null, 204)
        def targetAUri = post("/domains/$domainId/$type.pluralTerm", [
            name: "linking target A",
            owner: [targetUri: "/units/$unitId"],
            subType: "STA",
            status: "NEW",
        ]).location
        def targetBUri = post("/domains/$domainId/$type.pluralTerm", [
            name: "linking target A",
            owner: [targetUri: "/units/$unitId"],
            subType: "STA",
            status: "NEW",
        ]).location
        def elementUri = post("/domains/$domainId/$type.pluralTerm", [
            name: "linking source",
            owner: [targetUri: "/units/$unitId"],
            subType: "STA",
            status: "NEW",
        ]).location
        def originalElementEtag = get(elementUri).getETag()

        when: "adding links"
        post(elementUri + "/links", [
            linkTypeA: [
                [
                    target: [targetInDomainUri: targetAUri]
                ]
            ],
            linkTypeB: [
                [
                    target: [targetInDomainUri: targetBUri],
                    attributes: [
                        propOfTruth: true
                    ]
                ]
            ]
        ], 204)
        def returnedEtag = post(elementUri + "/links", [
            linkTypeA: [
                [
                    target: [targetInDomainUri: targetBUri]
                ]
            ]
        ], 204).getETag()

        then: "etag and links have been updated"
        returnedEtag != originalElementEtag
        with(get(elementUri)) {
            getETag() == returnedEtag
            with(body.links) {
                linkTypeA.size() == 2
                linkTypeA.find { it.target.targetInDomainUri.endsWith(targetAUri) } != null
                linkTypeA.find { it.target.targetInDomainUri.endsWith(targetBUri) } != null
                linkTypeB.size() == 1
                linkTypeB.find { it.target.targetInDomainUri.endsWith(targetBUri) }.attributes.propOfTruth
            }
        }

        expect: "adding an invalid link type to fail"
        post(elementUri + "/links", [
            sillyLink: [
                [
                    target: [targetInDomainUri: targetAUri]
                ]
            ],
        ], 400).body.message == "Link type 'sillyLink' is not defined for element type '$type.singularTerm'"

        and: "adding an existing link to fail"
        post(elementUri + "/links", [
            linkTypeA: [
                [
                    target: [targetInDomainUri: targetAUri]
                ]
            ],
        ], 409).body.message ==~ /Link with type 'linkTypeA' and target ID .+ already exists/

        where:
        type << ElementType.values()
    }

    def "link definition must have a sub type"() {
        expect:
        put("/content-creation/domains/$domainId/element-type-definitions/asset", [
            links: [
                badLink: [
                    targetType: "asset"
                ]
            ]
        ], null, 400).body['links[badLink].targetSubType'] == "must not be null"
    }
}
