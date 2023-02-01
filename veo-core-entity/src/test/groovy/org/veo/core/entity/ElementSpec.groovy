/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Jonas Jordan.
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

import spock.lang.Specification

class ElementSpec extends Specification {
    def "formats complete display name"() {
        given: "an Element spy"
        def spy = Spy(Element)

        when: "setting abbreviation, name, and designator"
        spy.abbreviation >> "FF"
        spy.name >> "Fun foo"
        spy.designator >> 'XXX-23'

        then: "all three elements appear in the display name"
        spy.displayName == "XXX-23 FF Fun foo"
    }

    def "formats display name without abbreviation"() {
        given: "an Element spy"
        def spy = Spy(Element)

        when: "setting the abbreviation to null"
        spy.abbreviation >> null
        spy.name >> "Fun foo"
        spy.designator >> 'XXX-42'

        then: "the abbreviation is omitted"
        spy.displayName == "XXX-42 Fun foo"
    }
}
