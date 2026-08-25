/*
 * verinice.veo
 * Copyright (C) 2026  Jonas Jordan
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
package org.veo.core.entity

import static java.util.Locale.ENGLISH
import static java.util.Locale.GERMAN

import org.veo.core.entity.type.VeoType
import org.veo.core.entity.type.VeoType as T

import spock.lang.Specification

class VeoTypeSpec extends Specification {
    def '#typeA includes #typeB'() {
        expect:
        typeA.includes(typeB)
        typeA.intersectsWith (typeB)
        typeB.intersectsWith (typeA)

        where:
        typeA                                                             | typeB
        T.mapOf(T.string(), T.string())                                   | T.mapOf(T.string(), T.string())
        T.mapOf(T.string(), T.string().orNothing())                       | T.mapOf(T.string(), T.string())
        T.mapOf(T.string(), T.string()).orNothing()                       | T.mapOf(T.string(), T.string())
        T.mapOf(T.sumOf(T.string(), T.integer()), T.string()).orNothing() | T.mapOf(T.string(), T.string()).orNothing()
        T.string()                                                        | T.durationString()
        T.attributeContainer([foo: T.string(), bar: T.integer()])         | T.attributeContainer([foo: T.string()])
        T.attributeContainer([foo: T.string().orNothing()])               | T.attributeContainer([foo: T.string()])
    }

    def '#typeA does not include #typeB'() {
        expect:
        !typeA.includes(typeB)

        where:
        typeA                                                 | typeB
        T.mapOf(T.string(), T.string())                       | T.mapOf(T.string(), T.integer())
        T.mapOf(T.string(), T.string())                       | T.mapOf(T.integer(), T.string())
        T.mapOf(T.string(), T.string())                       | T.mapOf(T.string(), T.string().orNothing())
        T.mapOf(T.string(), T.string())                       | T.mapOf(T.sumOf(T.string(), T.integer()), T.string())
        T.mapOf(T.string(), T.string())                       | T.mapOf(T.string(), T.string().orNothing())
        T.mapOf(T.string(), T.string())                       | T.mapOf(T.string(), T.string()).orNothing()
        T.mapOf(T.sumOf(T.string(), T.integer()), T.string()) | T.mapOf(T.string(), T.string()).orNothing()
        T.durationString()                                    | T.string()
        T.attributeContainer([foo: T.string()])               | T.attributeContainer([foo: T.integer()])
        T.attributeContainer([foo: T.string()])               | T.attributeContainer([foo: T.integer(), bar: T.bool()])
    }

    def '#typeA and #typeB intersect'() {
        expect:
        typeA.intersectsWith(typeB)
        typeB.intersectsWith(typeA)

        where:
        typeA                                                           | typeB
        T.mapOf(T.sumOf(T.bool(), T.integer()), T.string())             | T.mapOf(T.sumOf(T.bool(), T.string()), T.string())
        T.mapOf(T.sumOf(T.bool(), T.integer()), T.string())             | T.mapOf(T.sumOf(T.bool(), T.string()), T.string()).orNothing()
        T.mapOf(T.sumOf(T.bool(), T.integer()), T.string()).orNothing() | T.mapOf(T.sumOf(T.bool(), T.string()), T.string())
        T.mapOf(T.sumOf(T.bool(), T.integer()), T.string()).orNothing() | T.mapOf(T.sumOf(T.bool(), T.string()), T.string()).orNothing()
        T.mapOf(T.string(), T.sumOf(T.bool(), T.integer()))             | T.mapOf(T.string(), T.sumOf(T.bool(), T.string()))
        T.mapOf(T.string(), T.sumOf(T.bool(), T.integer()))             | T.mapOf(T.string(), T.sumOf(T.bool(), T.string())).orNothing()
        T.mapOf(T.string(), T.sumOf(T.bool(), T.integer()).orNothing()) | T.mapOf(T.string(), T.sumOf(T.bool(), T.string()))
        T.mapOf(T.string(), T.sumOf(T.bool(), T.integer()).orNothing()) | T.mapOf(T.string(), T.sumOf(T.bool(), T.string())).orNothing()
        T.durationString()                                              | T.string()
        T.attributeContainer([foo: T.sumOf(T.string(), T.bool())])      | T.attributeContainer([foo: T.sumOf(T.string(), T.decimal())])
        T.attributeContainer([foo: T.bool(), bar: T.string()])          | T.attributeContainer([foo: T.bool(), tar: T.decimal()])
    }

    def '#typeA and #typeB do not intersect'() {
        expect:
        !typeA.intersectsWith(typeB)
        !typeB.intersectsWith(typeA)
        !typeB.includes(typeA)
        !typeA.includes(typeB)

        where:
        typeA                                               | typeB
        T.mapOf(T.sumOf(T.bool(), T.integer()), T.string()) | T.mapOf(T.sumOf(T.string(), T.decimal()), T.string())
        T.mapOf(T.sumOf(T.bool(), T.integer()), T.string()) | T.mapOf(T.sumOf(T.bool(), T.string()), T.integer())
        T.mapOf(T.string(), T.sumOf(T.bool(), T.integer())) | T.mapOf(T.string(), T.sumOf(T.string(), T.decimal()))
        T.mapOf(T.string(), T.sumOf(T.bool(), T.integer())) | T.mapOf(T.integer(), T.sumOf(T.bool(), T.string()))
    }

    def "maps must not contain null keys"() {
        when:
        VeoType.mapOf(VeoType.nothing(), VeoType.string())

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == "map keys cannot be null: expected *, got Null"

        when:
        VeoType.mapOf(VeoType.string().orNothing(), VeoType.string())

        then:
        ex = thrown(IllegalArgumentException)
        ex.message == "map keys cannot be null: expected *, got String|Null"
    }

    def "#type values #inValues are compared correctly"() {
        expect:
        inValues.toSorted(type.comparator) == outValues

        where:
        type                           | inValues                           | outValues
        T.integer()                    | [15, 7000, 459, 12, 57]            | [12, 15, 57, 459, 7000]
        T.string()                     | ["b", "c", "a"]                    | ["a", "b", "c"]
        T.durationString()             | ["PT12H", "PT30S", "PT45S"]        | ["PT30S", "PT45S", "PT12H"]
        T.durationString().orNothing() | [
            "P100M",
            "P100MT2S",
            null,
            "P2Y"
        ] | [
            "P2Y",
            "P100M",
            "P100MT2S",
            null
        ]
        T.integer().orNothing()        | [16, null, 12, null]               | [12, 16, null, null]
        T.nothing()                    | [null, null, null]                 | [null, null, null]
    }

    def "#type values cannot be compared"() {
        when:
        type.comparator

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == expectedError

        where:
        type                            | expectedError
        T.sumOf(T.integer(), T.bool())  | "cannot compare values with different types (Boolean|Integer)"
        T.element()                     | "comparison is not supported for Element"
        T.element().orNothing()         | "comparison is not supported for Element"
        T.element(ElementType.INCIDENT) | "comparison is not supported for Incident"
        T.attributeContainer([:])       | "comparison is not supported for AttributeContainer<>"
    }

    def '#typeA == #typeB'() {
        expect:
        typeA == typeB
        typeB == typeA
        typeA.includes(typeB)
        typeB.includes(typeA)
        typeA.intersectsWith(typeB)
        typeB.intersectsWith(typeA)

        where:
        typeA                         | typeB
        T.string()                    | T.string()
        T.string().orNothing()        | T.string().orNothing()
        T.sumOf(T.string(), T.bool()) | T.sumOf(T.string(), T.bool())
        T.sumOf(T.string(), T.bool()) | T.sumOf(T.bool(), T.string())
    }

    def '#typeA != #typeB'() {
        expect:
        typeA != typeB
        typeB != typeA

        where:
        typeA                         | typeB
        T.bool()                      | T.integer()
        T.string().orNothing()        | T.string()
        T.sumOf(T.string(), T.bool()) | T.sumOf(T.string(), T.decimal())
    }

    def "#type value #value is formatted correctly in #locale"(VeoType type, Object value, Locale locale, String out) {
        expect:
        type.format(value,locale) == out

        where:
        type                          | value       | locale  | out
        VeoType.nothing()             | null        | ENGLISH | "undefined"
        VeoType.string()              | "foot"      | ENGLISH | "foot"
        VeoType.integer()             | 56          | ENGLISH | "56"
        VeoType.bool()                | false       | ENGLISH | "no"
        VeoType.bool()                | true        | ENGLISH | "yes"
        VeoType.bool()                | true        | GERMAN  | "ja"
        VeoType.decimal()             | 5.6         | ENGLISH | "5.6"
        VeoType.decimal()             | 5.6         | GERMAN  | "5,6"
        VeoType.decimal().orNothing() | 5.6         | GERMAN  | "5,6"
        VeoType.decimal().orNothing() | null        | GERMAN  | "unbestimmt"
        VeoType.durationString()      | "P2W4DT5M"  | ENGLISH | "18 days, 5 minutes"
        VeoType.durationString()      | "P2DT4H12M" | ENGLISH | "2 days, 4 hours, 12 minutes"
        VeoType.durationString()      | "P2DT4H12M" | GERMAN  | "2 Tage, 4 Stunden, 12 Minuten"
        VeoType.durationString()      | "PT10H0M2S" | GERMAN  | "10 Stunden, 2 Sekunden"
        VeoType.durationString()      | "P0D"       | GERMAN  | "0 Sekunden"
    }
}
