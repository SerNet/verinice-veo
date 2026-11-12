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
package org.veo.core.entity.condition

import org.veo.core.entity.Domain
import org.veo.core.entity.ElementType
import org.veo.core.entity.event.ElementEvent
import org.veo.core.entity.type.VeoType as T

import spock.lang.Specification

class EqualsExpressionSpec extends Specification {

    def "equals expression is valid for #leftType and #rightType"() {
        given:
        def domain = Mock(Domain) {}
        def exp = new EqualsExpression(
                Mock(CustomAspectAttributeValueExpression) {
                    getValueType(domain, ElementType.ASSET) >> leftType
                },
                Mock(ConstantExpression) {
                    getValueType(domain, ElementType.ASSET) >> rightType
                },
        )

        when:
        exp.selfValidate(domain, ElementType.ASSET)

        then:
        noExceptionThrown()
        exp.getValueType(domain, ElementType.ASSET) == T.bool()

        when: "inverting the comparison"
        exp = new EqualsExpression(exp.right, exp.left)

        and:
        exp.selfValidate(domain, ElementType.ASSET)

        then:
        noExceptionThrown()
        exp.getValueType(domain, ElementType.ASSET) == T.bool()

        where:
        leftType                                 | rightType
        T.integer()                              | T.integer()
        T.integer().orNothing()                  | T.integer()
        T.integer().orNothing()                  | T.sumOf(T.integer(), T.string())
        T.sumOf(T.integer(), T.bool())           | T.integer()
        T.sumOf(T.integer(), T.bool())           | T.bool()
        T.sumOf(T.integer(), T.bool())           | T.bool().orNothing()
        T.sumOf(T.integer(), T.bool())           | T.sumOf(T.bool(), T.string())
        T.listOf(T.integer())                    | T.listOf(T.integer())
        T.listOf(T.integer())                    | T.listOf(T.integer().orNothing())
        T.listOf(T.sumOf(T.integer(), T.bool())) | T.listOf(T.sumOf(T.string(), T.bool()))
        T.sumOf(T.bool(), T.listOf(T.integer())) | T.sumOf(T.string(), T.listOf(T.integer().orNothing()))
    }

    def "equals expression is invalid for #leftType and #rightType"() {
        given:
        def domain = Mock(Domain) {}
        def exp = new EqualsExpression(
                Mock(CustomAspectAttributeValueExpression) {
                    getValueType(domain, ElementType.ASSET) >> leftType
                },
                Mock(ConstantExpression) {
                    getValueType(domain, ElementType.ASSET) >> rightType
                },
        )

        when:
        exp.selfValidate(domain, ElementType.ASSET)

        then:
        IllegalArgumentException illEx = thrown()
        illEx.message == expectedError

        when: "inverting the comparison"
        exp = new EqualsExpression(exp.right, exp.left)

        and:
        exp.selfValidate(domain, ElementType.ASSET)

        then:
        thrown(IllegalArgumentException)

        where:
        leftType                      | rightType   | expectedError
        T.integer()                   | T.nothing() | "given values cannot be equal: Integer does not intersect with Null"
        T.integer()                   | T.string()  | "given values cannot be equal: Integer does not intersect with String"
        T.integer().orNothing()       | T.string()  | "given values cannot be equal: Integer|Null does not intersect with String"
        T.sumOf(T.bool(), T.string()) | T.number()  | "given values cannot be equal: Boolean|String does not intersect with BigDecimal|Integer|Long"
        T.listOf(T.integer())         | T.integer() | "given values cannot be equal: List<Integer> does not intersect with Integer"
    }

    def "sub expressions are considered when handling events"() {
        given:
        def domain = Mock(Domain) {}
        def event = Mock(ElementEvent)
        def exp = new EqualsExpression(
                Mock(CustomAspectAttributeValueExpression) {
                    isAffectedByEvent(event, domain) >> left
                },
                Mock(ConstantExpression) {
                    isAffectedByEvent(event, domain) >> right
                },
        )

        expect:
        exp.isAffectedByEvent(event, domain) == affected

        where:
        left  | right | affected
        false | false | false
        false | true  | true
        true  | false | true
        true  | true  | true
    }
}
