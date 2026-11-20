/*
 * verinice.veo
 * Copyright (C) 2026  Jochen Kemnade.
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
 */
package org.veo.persistence.entity

import org.veo.persistence.entity.jpa.JsonStructureMutabilityPlan

import spock.lang.Specification

class JsonStructureMutabilityPlanSpec extends Specification {

    def plan = new JsonStructureMutabilityPlan()

    def "the copy equals the original and shares no map or list with it"() {
        given:
        def original = [a: [b: [1, 2, [c: "d"]]], e: "f"]

        when:
        def copy = plan.deepCopy(original)

        then:
        copy == original
        !copy.is(original)
        !copy.a.is(original.a)
        !copy.a.b.is(original.a.b)
        !copy.a.b[2].is(original.a.b[2])
    }

    def "strings are shared, not copied"() {
        given:
        def text = new String("a text that may be large")
        def original = [text: text, nested: [list: [text]]]

        when:
        def copy = plan.deepCopy(original)

        then:
        copy.text.is(text)
        copy.nested.list[0].is(text)
    }

    def "changes of the original in place are not visible in the copy"() {
        given:
        def original = [a: [b: 1], list: [1], gone: "x"]
        def copy = plan.deepCopy(original)

        when:
        original.a.b = 2
        original.list << 2
        original.remove("gone")

        then:
        copy == [a: [b: 1], list: [1], gone: "x"]
        copy != original
    }

    def "null stays null"() {
        expect:
        plan.deepCopy(null) == null
    }

    def "all types of parsed JSON are accepted"() {
        given:
        def original = [
            text: "a",
            integer: 1,
            long: 2L,
            decimal: 1.5d,
            bigDecimal: new BigDecimal("3.14"),
            flag: true,
            nothing: null,
            list: [1, "b", [c: false], null],
            nested: [d: [e: "f"]]
        ]

        expect:
        plan.deepCopy(original) == original
    }

    def "a value that it cannot copy is rejected, wherever it is"() {
        when:
        plan.deepCopy(value)

        then:
        def e = thrown(IllegalArgumentException)
        e.message.contains(type)

        where:
        value                             | type
        [a: new Date()]                   | "java.util.Date"
        [a: [b: [new StringBuilder()]]]   | "java.lang.StringBuilder"
        [a: new Object()]                 | "java.lang.Object"
        [(UUID.randomUUID()): "x"]        | "java.util.UUID"
        [a: [(1): "x"]]                   | "java.lang.Integer"
    }
}
