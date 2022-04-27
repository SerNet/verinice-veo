/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Jonas Jordan
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


import spock.lang.Specification

class GreaterThanMatcherSpec extends Specification{

    def "fractional comparison values are supported"() {
        given:
        def matcher = new GreaterThanMatcher(new BigDecimal("12.453"))

        expect: "decimals to work"
        !matcher.matches(new BigDecimal("12.452"))
        !matcher.matches(new BigDecimal("12.452999999999999999999999999999999999999999"))
        !matcher.matches(new BigDecimal("12.453"))
        matcher.matches(new BigDecimal("12.453000000000000000000000000000000000000001"))
        matcher.matches(new BigDecimal("12.454"))

        and: "integers"
        !matcher.matches(11)
        !matcher.matches(12)
        matcher.matches(13)
        matcher.matches(14)

        and: "longs"
        !matcher.matches(11l)
        !matcher.matches(12l)
        matcher.matches(13l)
        matcher.matches(14l)
    }

    def "integer comparison values are supported"() {
        given:
        def matcher = new GreaterThanMatcher(new BigDecimal(12))

        expect: "decimals to work"
        !matcher.matches(new BigDecimal("11"))
        !matcher.matches(new BigDecimal("11.9999999999999999999999999999999999"))
        !matcher.matches(new BigDecimal("12"))
        matcher.matches(new BigDecimal("12.0000000000000000000000000000000001"))
        matcher.matches(new BigDecimal("13"))

        and: "integers"
        !matcher.matches(11)
        !matcher.matches(12)
        matcher.matches(13)
        matcher.matches(14)

        and: "longs"
        !matcher.matches(11l)
        !matcher.matches(12l)
        matcher.matches(13l)
        matcher.matches(14l)
    }
}
