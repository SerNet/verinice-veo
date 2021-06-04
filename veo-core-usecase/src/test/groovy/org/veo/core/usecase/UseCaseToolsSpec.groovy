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
package org.veo.core.usecase

import static org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.Test

import org.veo.core.entity.CatalogItem
import org.veo.core.entity.Control
import org.veo.core.entity.CustomLink
import org.veo.core.entity.Key
import org.veo.core.entity.TailoringReference
import org.veo.core.entity.TailoringReferenceType

import spock.lang.Specification

class UseCaseToolsSpec extends Specification{
    TailoringReference r1 = Mock()
    TailoringReference r2 = Mock()
    TailoringReference r3 = Mock()
    TailoringReference r4 = Mock()
    TailoringReference r5 = Mock()

    CatalogItem item = Mock()
    CatalogItem item1 = Mock()
    CatalogItem item2 = Mock()

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

        r1.referenceType >> TailoringReferenceType.COPY
        r1.catalogItem >> item

        r2.referenceType >> TailoringReferenceType.OMIT
        r2.catalogItem >> item

        r3.referenceType >> TailoringReferenceType.LINK
        r3.catalogItem >> item

        r4.referenceType >> TailoringReferenceType.LINK
        r4.catalogItem >> item1

        r5.referenceType >> TailoringReferenceType.COPY_ALWAYS
        r5.catalogItem >> item1

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


    def "order links"() {
        when:

        def list = [l2, l1, l3] as List
        list.sort(UseCaseTools.BY_LINK_EXECUTION)

        then:
        list[0] == l1
        list[0].target == element
        list[1] == l2
        list[2] == l3

        when:

        list = [l3, l2, l1] as List
        list.sort(UseCaseTools.BY_LINK_EXECUTION)

        then:
        list[0] == l1
        list[0].target == element
        list[1] == l2
        list[2] == l3

        when:
        list = [lt3, lt2, lt1] as List
        list.sort(UseCaseTools.BY_LINK_EXECUTION)

        then:
        list[0] == lt2
        list[1] == lt3
        list[2] == lt1
    }

    def "order tailor ref"() {
        when:
        def list = [r1, r2, r3] as List
        list.sort(UseCaseTools.BY_EXECUTION)

        then:

        list[0].referenceType == TailoringReferenceType.OMIT
        list[1].referenceType == TailoringReferenceType.LINK
        list[2].referenceType == TailoringReferenceType.COPY

        when:

        def list1 = [r1, r2, r4, r3] as List
        list1.sort(UseCaseTools.BY_EXECUTION)

        then:

        list1[0].referenceType == TailoringReferenceType.OMIT
        list1[1].referenceType == TailoringReferenceType.LINK
        list1[1].catalogItem == item
        list1[2].referenceType == TailoringReferenceType.LINK
        list1[3].referenceType == TailoringReferenceType.COPY
    }

    def "filter copy TailoringReference"() {
        expect:

        UseCaseTools.IS_COPY_PREDICATE.test(r1)
        !UseCaseTools.IS_COPY_PREDICATE.test(r2)
        !UseCaseTools.IS_COPY_PREDICATE.test(r3)
        UseCaseTools.IS_COPY_PREDICATE.test(r5)
    }

    def "filter link TailoringReference"() {
        expect:

        !UseCaseTools.IS_LINK_PREDICATE.test(r1)
        !UseCaseTools.IS_LINK_PREDICATE.test(r2)
        UseCaseTools.IS_LINK_PREDICATE.test(r3)
        !UseCaseTools.IS_LINK_PREDICATE.test(r5)
    }

    def "getElementsToCreate"() {
        when:
        item.tailoringReferences >> []
        def elements = UseCaseTools.getElementsToCreate(item)

        then:
        elements.size() == 0

        when:
        item2.tailoringReferences >> [r1, r5, r2]
        item1.tailoringReferences >> [r1]

        elements = UseCaseTools.getElementsToCreate(item2)
        then:
        elements.size() == 2
        elements.contains(item)
        elements.contains(item1)
    }
}
