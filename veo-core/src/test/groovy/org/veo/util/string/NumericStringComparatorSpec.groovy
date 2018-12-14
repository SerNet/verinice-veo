/*******************************************************************************
 * Copyright (c) 2018 Jochen Kemnade.
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.veo.util.string;

import spock.lang.Specification
import spock.lang.Unroll

class NumericStringComparatorSpec extends Specification {

    @Unroll
    def "#substring should be a substring of #s which starts at position #start and ends at #end" () {
        expect:

        AbstractNumericStringComparator.getSubstring(s,start,end) == substring
        where:
        s                               | start | end | substring
        'TimeFormatterSpec'             | 4     | 13  | 'Formatter'
        'NumericStringComparatorSpec'   | 0     | 7   | 'Numeric'
        'Foo'                           | 0     | 0   | ''
        'spock.lang.Specification'      | 11    | -1  | 'Specification'
        'org.veo.util.string'           | 0     | 19  | 'org.veo.util.string'
    }
}
