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
