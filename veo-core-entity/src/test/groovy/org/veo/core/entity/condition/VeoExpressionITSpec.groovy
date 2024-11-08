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
}
