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
import org.veo.core.entity.Element
import org.veo.core.entity.ElementType
import org.veo.core.entity.event.ElementEvent
import org.veo.core.entity.type.VeoType as T

import spock.lang.Specification

class TernaryExpressionSpec extends Specification {

    def "Ternary expression returns expected result"() {
        given:
        Domain domain = Mock()
        Element element = Mock()

        ConstantExpression conditionExpression = Stub{
            getValue(element, domain) >> conditionValue
        }
        CustomAspectAttributeValueExpression thenExpression = Stub{
            getValue(element, domain) >> thenValue
        }
        ConstantExpression elseExpression = Stub{
            getValue(element, domain) >> elseValue
        }
        TernaryExpression expression = new TernaryExpression(conditionExpression, thenExpression, elseExpression)

        expect:
        expression.getValue(element, domain) == result

        where:
        conditionValue | thenValue | elseValue| result
        true | 3 | 4 | 3
        false | 3 | 4 | 4
        Boolean.TRUE | 3 | 4 | 3
        Boolean.FALSE | 3 | 4 | 4
        null | 3 | 4 | 4
        "true" | 3 | 4 | 4
    }

    def "Ternary validates correctly with different value types"() {
        given:
        Domain domain = Mock()
        ElementType elementType = Mock()

        ConstantExpression conditionExpression = Stub{
            getValueType(domain, elementType) >> T.bool()
        }
        CustomAspectAttributeValueExpression thenExpression = Stub {
            getValueType(domain, elementType) >> thenValueType
        }

        ConstantExpression elseExpression = Stub {
            getValueType(domain, elementType) >> elseValueType
        }

        TernaryExpression expression = new TernaryExpression(conditionExpression, thenExpression, elseExpression)

        when:
        expression.selfValidate(domain, elementType)

        then:
        noExceptionThrown()

        where:
        thenValueType | elseValueType
        T.string()    | T.string()
        T.string()    | T.nothing()
        T.nothing()   | T.string()
    }

    def "Ternary supports two different result types"() {
        given:
        Domain domain = Mock()
        ElementType elementType = Mock()

        ConstantExpression conditionExpression = Stub{
            getValueType(domain, elementType) >> T.bool()
        }
        CustomAspectAttributeValueExpression thenExpression = Stub {
            getValueType(domain, elementType) >> thenValueType
        }

        ConstantExpression elseExpression = Stub {
            getValueType(domain, elementType) >> elseValueType
        }

        TernaryExpression expression = new TernaryExpression(conditionExpression, thenExpression, elseExpression)

        when:
        expression.selfValidate(domain, elementType)

        then:
        noExceptionThrown()
        expression.getValueType(domain, elementType) == T.sumOf([thenValueType, elseValueType])

        where:
        thenValueType | elseValueType
        T.integer()   | T.string()
        T.string()    | T.listOf(T.string())
    }

    def "sub expressions are considered when handling events"() {
        given:
        def domain = Mock(Domain) {}
        def event = Mock(ElementEvent)
        def exp = new TernaryExpression(
                Mock(CustomAspectAttributeValueExpression) {
                    isAffectedByEvent(event, domain) >> inputsAffected[0]
                },
                Mock(ConstantExpression) {
                    isAffectedByEvent(event, domain) >> inputsAffected[1]
                },
                Mock(ConstantExpression) {
                    isAffectedByEvent(event, domain) >> inputsAffected[2]
                },
        )

        expect:
        exp.isAffectedByEvent(event, domain) == inputsAffected.any{it}

        where:
        inputsAffected << [
            [false, true],
            [false, true],
            [false, true]
        ].combinations()
    }
}
