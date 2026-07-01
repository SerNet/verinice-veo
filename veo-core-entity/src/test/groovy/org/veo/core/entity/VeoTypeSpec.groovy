/*******************************************************************************
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
 ******************************************************************************/
package org.veo.core.entity

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
}
