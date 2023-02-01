/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Urs Zeidler.
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
package org.veo.core.entity

import org.veo.core.entity.util.CustomLinkComparators

import spock.lang.Specification

class CustomLinkComparatorsSpec extends Specification {
    CatalogItem item = Mock()
    CatalogItem item1 = Mock()

    Control element = Mock()
    Control element1 = Mock()
    Control element2 = Mock()

    CustomLink l1 = Mock()
    CustomLink l2 = Mock()
    CustomLink l3 = Mock()

    CustomLink lt1 = Mock()
    CustomLink lt2 = Mock()
    CustomLink lt3 = Mock()

    def setup() {
        item.element >> element
        item1.element >> element1

        element.id >> Key.uuidFrom("00000000-0000-0000-0000-000000000001")
        element1.id >> Key.uuidFrom("00000000-0000-0000-0000-000000000002")
        element2.id >> Key.uuidFrom("00000000-0000-0000-0000-000000000003")

        l1.target >> element
        l2.target >> element1
        l3.target >> element2

        lt1.target >> element
        lt1.type >> "z"
        lt2.target >> element1
        lt2.type >> "a"
        lt3.target >> element2
        lt3.type >> "a"
    }

    def "compare strings null-safe"() {
        when:
        def list = ["banana", "apple", null]
        list.sort(CustomLinkComparators.BY_STRING_NULL_SAFE)

        then:
        list == [null, "apple", "banana"]
    }

    def "order links"() {
        when:
        def list = [l2, l1, l3] as List
        list.sort(CustomLinkComparators.BY_LINK_EXECUTION)

        then:
        list[0] == l1
        list[0].target == element
        list[1] == l2
        list[2] == l3

        when:
        list = [l3, l2, l1] as List
        list.sort(CustomLinkComparators.BY_LINK_EXECUTION)

        then:
        list[0] == l1
        list[0].target == element
        list[1] == l2
        list[2] == l3

        when:
        list = [lt3, lt2, lt1] as List
        list.sort(CustomLinkComparators.BY_LINK_EXECUTION)

        then:
        list[0] == lt2
        list[1] == lt3
        list[2] == lt1
    }
}
