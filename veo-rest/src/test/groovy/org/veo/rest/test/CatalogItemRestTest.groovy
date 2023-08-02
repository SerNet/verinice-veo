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

class CatalogItemRestTest extends VeoRestTest{
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
    }
}
