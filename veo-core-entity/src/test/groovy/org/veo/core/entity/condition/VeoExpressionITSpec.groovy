/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2024  Jochen Kemnade
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
package org.veo.core.entity.condition

import org.veo.core.entity.CustomAspect
import org.veo.core.entity.Domain
import org.veo.core.entity.Element

import spock.lang.Specification

class VeoExpressionITSpec extends Specification {

    def "Remove #value from #list returns #result"() {
        given:
        Domain domain = Mock()
        Element element = Mock()
        CustomAspectAttributeValueExpression from = Stub{
            getValue(element, domain) >> list
        }
        ConstantExpression valueExpression = Stub{
            getValue(element, domain) >> value
        }
        RemoveExpression expression = new RemoveExpression(from, valueExpression)

        expect:
        expression.getValue(element, domain) == result

        where:
        list | value | result
        null | 3 | null
        [] | 3 | []
        [1, 2, 3] | 3 | [1, 2]
        [1, 2, 3] | null | [1, 2, 3]
        ['f', 'o', 'o'] | 'o' | ['f']
    }

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

    def "Map a list using a map"() {
        given:
        Domain domain = Mock()
        Element element = Mock()
        CustomAspectAttributeValueExpression source = Stub{
            getValue(element, domain) >> list
        }
        ConstantExpression mapping = Stub{
            getValue(element, domain) >> map
        }
        MapExpression expression = new MapExpression(source, mapping)

        expect:
        expression.getValue(element, domain) == result

        where:
        list | map | result
        null | [:] | null
        [] | [:] | []
        [1, 2, 3] | [1:2 , 3:2] | [2, null, 2]
        ['foo', 'bar'] | [bar: 'baz'] | [null, 'baz']
    }

    def "Read same attribute from different domains"() {
        given:
        Domain domain1 = Mock()
        CustomAspect ca1 = Stub {
            getType() >> 'ca'
            getAttributes()>> [attr: 'v1']
        }
        Domain domain2 = Mock()
        CustomAspect ca2 = Stub {
            getType() >> 'ca'
            getAttributes()>> [attr: 'v2']
        }
        Element element = Stub {
            getCustomAspects(domain1)>> [ca1]
            getCustomAspects(domain2)>> [ca2]
        }

        CustomAspectAttributeValueExpression e = new CustomAspectAttributeValueExpression('ca', 'attr' )

        expect:
        e.getValue(element, domain1) == 'v1'
        e.getValue(element, domain2) == 'v2'
    }
}
