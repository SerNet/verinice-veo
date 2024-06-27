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
import static org.veo.rest.test.UserType.ADMIN
import static org.veo.rest.test.UserType.CONTENT_CREATOR
import static org.veo.rest.test.UserType.SECONDARY_CLIENT_USER

class CatalogItemRestTest extends VeoRestTest {
    def "fetch catalog items"() {
        expect: "items are fetched"
        with(get("/domains/$testDomainId/catalog-items").body) {
            totalItemCount == 7
            pageCount == 1
            page == 0
            items.size() == 7
            with(items.find { it.abbreviation == "c-2" }) {
                UUID.fromString(id)
                name == "Control-2"
                abbreviation == "c-2"
                description ==~ /Lorem ipsum .*/
                elementType == "control"
                subType == "TOM"
                owner.owner.get(_self).body == it
            }
        }

        and: "pagination works"
        with(get("/domains/$testDomainId/catalog-items?size=3").body) {
            totalItemCount == 7
            pageCount == 3
            page == 0
            items*.name == [
                "Control-1",
                "Control-2",
                "Control-3"
            ]
        }
        with(get("/domains/$testDomainId/catalog-items?size=3&page=1").body) {
            totalItemCount == 7
            pageCount == 3
            page == 1
            items*.name == [
                "Control-4",
                "Control-cc-1",
                "Control-cc-2"
            ]
        }
        with(get("/domains/$testDomainId/catalog-items?size=3&page=2").body) {
            totalItemCount == 7
            pageCount == 3
            page == 2
            items*.name == ["General server"]
        }

        and: "filters work"
        with(get("/domains/$testDomainId/catalog-items?elementType=asset").body) {
            totalItemCount == 1
            items*.name == ["General server"]
        }
        with(get("/domains/$testDomainId/catalog-items?subType=TOM").body) {
            totalItemCount == 6
            items*.name.every { it.startsWith("Control") }
        }
        with(get("/domains/$testDomainId/catalog-items?name=Control-2").body) {
            totalItemCount == 1
            items[0].abbreviation == "c-2"
        }
        with(get("/domains/$testDomainId/catalog-items?description=Lorem").body) {
            totalItemCount == 6
            items[0].name == "Control-1"
            items*.description.every { it.startsWith("Lorem") }
        }
        with(get("/domains/$testDomainId/catalog-items?abbreviation=c-2").body) {
            totalItemCount == 2
            items*.name ==~ ["Control-2", "Control-cc-2"]
            items*.abbreviation ==~ ["c-2", "cc-2"]
        }
    }

    def "catalog item IDs are symbolic"() {
        given: "a catalog item in a copy of test-domain"
        def copyOfTestDomainId = copyDomain(testDomainId)
        def sourceUnitId = postNewUnit().resourceId
        post("/domains/$copyOfTestDomainId/assets", [
            name: "brave test subject",
            subType: "Information",
            status: "CURRENT",
            owner: [targetUri: "/units/$sourceUnitId"]
        ])
        def originalCatalogItemId = get("/domains/$copyOfTestDomainId/catalog-items?sortBy=abbreviation").body.items
                .find { it.abbreviation == "aa-1" }
                .id

        expect:
        originalCatalogItemId != null

        when: "creating and exporting a domain template from the domain"
        def templateId = post("/content-creation/domains/$copyOfTestDomainId/template", [
            version: "99.99.99"
        ], 201, CONTENT_CREATOR).body.id
        def exportedDomainTemplate = get("/content-creation/domain-templates/$templateId", 200, CONTENT_CREATOR).body

        and: "importing the template under a different name"
        exportedDomainTemplate.name = "completely different domain template ${randomUUID()}"
        def newDomainTemplateId = post("/content-creation/domain-templates", exportedDomainTemplate, 201, CONTENT_CREATOR).body.resourceId

        and: "applying the template in another client"
        post("/domain-templates/$newDomainTemplateId/createdomains", null, 204, ADMIN)
        def newDomainInOtherClientId = get("/domains", 200, SECONDARY_CLIENT_USER).body.find { it.name == exportedDomainTemplate.name }.id

        then: "the catalog item can be found in the new domain under the original symbolic ID"
        with(get("/domains/$newDomainInOtherClientId/catalog-items/$originalCatalogItemId", 200, SECONDARY_CLIENT_USER).body) {
            abbreviation == "aa-1"
            id == originalCatalogItemId
        }
    }
}
