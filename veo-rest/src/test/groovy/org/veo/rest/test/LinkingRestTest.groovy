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

import org.veo.core.entity.EntityType

class LinkingRestTest extends VeoRestTest{

    def "asset can be linked with #targetType.pluralTerm"() {
        given: "a target element"
        def domainId = post("/content-creation/domains", [
            name: randomUUID(),
            authority: "me",
        ], 201, CONTENT_CREATOR).body.resourceId
        put("/content-creation/domains/$domainId/element-type-definitions/asset", [
            subTypes: [
                ST: [statuses: ["NEW"]]
            ],
            links: [
                someLink: [
                    targetType: targetType.singularTerm
                ]
            ]
        ], "", 204, CONTENT_CREATOR)
        def unitId = post("/units", [
            name: "some unit",
            domains: [
                [targetUri: "/domains/$domainId"]
            ]
        ]).body.resourceId
        def targetId = post("/$targetType.pluralTerm", [
            name: "target element",
            owner: [targetUri: "/units/$unitId"]
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
            links.someLink[0].target.targetInDomainUri ==~ /.*\/domains\/$domainId\/$targetType.pluralTerm\/$targetId/
            links.someLink[0].target.displayName ==~ /.* target element/
        }

        where:
        targetType << EntityType.ELEMENT_TYPES
    }
}
