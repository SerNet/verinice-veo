/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Jonas Jordan
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
package org.veo.rest

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.test.context.support.WithUserDetails

import org.veo.core.VeoMvcSpec
import org.veo.core.entity.Element
import org.veo.core.entity.EntityType
import org.veo.core.entity.Unit
import org.veo.core.repository.UnitRepository
import org.veo.persistence.access.ClientRepositoryImpl

@WithUserDetails("user@domain.example")
class PaginationMockMvcSpec extends VeoMvcSpec {
    @Autowired
    private ClientRepositoryImpl clientRepository
    @Autowired
    private UnitRepository unitRepository

    private Unit unit

    def setup() {
        def client= createTestClient()

        unit = unitRepository.save(newUnit(client) {
            name = "Test unit"
        })
    }

    def "paginates through all #type sorted by abbreviation"() {
        given:
        for(i in 1..5) {
            post("/$type", [
                name: "a good entity",
                abbreviation: "$i",
                owner: [
                    targetUri: "http://localhost/units/${unit.id.uuidValue()}"
                ]
            ])
        }
        expect: "pagination works when getting all elements"
        with(parseJson(get("/$type?size=2&sortBy=abbreviation&sortOrder=desc"))) {
            items*.abbreviation == ["5", "4"]
            page == 0
            totalItemCount == 5
            pageCount == 3
        }
        with(parseJson(get("/$type?size=2&sortBy=abbreviation&sortOrder=desc&page=1"))) {
            items*.abbreviation == ["3", "2"]
            page == 1
            totalItemCount == 5
            pageCount == 3
        }
        with(parseJson(get("/$type?size=2&sortBy=abbreviation&sortOrder=desc&page=2"))) {
            items*.abbreviation == ["1"]
            page == 2
            totalItemCount == 5
            pageCount == 3
        }
        with(parseJson(get("/$type?size=2&sortBy=abbreviation&sortOrder=desc&page=3"))) {
            items*.abbreviation == []
            page == 3
            totalItemCount == 5
            pageCount == 3
        }

        and: "big page sizes work"
        with(parseJson(get("/$type?size=5&sortBy=abbreviation&sortOrder=desc"))) {
            items*.abbreviation == ["5", "4", "3", "2", "1"]
            page == 0
            totalItemCount == 5
            pageCount == 1
        }
        with(parseJson(get("/$type?size=10&sortBy=abbreviation&sortOrder=desc"))) {
            items*.abbreviation == ["5", "4", "3", "2", "1"]
            page == 0
            totalItemCount == 5
            pageCount == 1
        }

        when: "setting page size to zero"
        get("/$type?size=0&sortBy=abbreviation&sortOrder=desc", 400)
        then:
        thrown(IllegalArgumentException)

        expect: "pagination works when running a search for all elements in the unit"
        def searchUri = parseJson(post("/$type/searches", [:])).searchUrl
        with(parseJson(get("$searchUri?size=2&sortBy=abbreviation&sortOrder=desc"))) {
            items*.abbreviation == ["5", "4"]
            page == 0
            totalItemCount == 5
            pageCount == 3
        }
        with(parseJson(get("$searchUri?size=2&sortBy=abbreviation&sortOrder=desc&page=1"))) {
            items*.abbreviation == ["3", "2"]
            page == 1
            totalItemCount == 5
            pageCount == 3
        }
        with(parseJson(get("$searchUri?size=2&sortBy=abbreviation&sortOrder=desc&page=2"))) {
            items*.abbreviation == ["1"]
            page == 2
            totalItemCount == 5
            pageCount == 3
        }
        with(parseJson(get("$searchUri?size=2&sortBy=abbreviation&sortOrder=desc&page=3"))) {
            items*.abbreviation == []
            page == 3
            totalItemCount == 5
            pageCount == 3
        }

        and: "big page sizes work"
        with(parseJson(get("$searchUri?size=5&sortBy=abbreviation&sortOrder=desc"))) {
            items*.abbreviation == ["5", "4", "3", "2", "1"]
            page == 0
            totalItemCount == 5
            pageCount == 1
        }
        with(parseJson(get("$searchUri?size=10&sortBy=abbreviation&sortOrder=desc"))) {
            items*.abbreviation == ["5", "4", "3", "2", "1"]
            page == 0
            totalItemCount == 5
            pageCount == 1
        }

        when: "setting page size to zero"
        get("$searchUri?size=0&sortBy=abbreviation&sortOrder=desc", 400)
        then:
        thrown(IllegalArgumentException)

        where:
        type << EntityType.values().findAll { Element.isAssignableFrom(it.type) }*.pluralTerm
    }
}
