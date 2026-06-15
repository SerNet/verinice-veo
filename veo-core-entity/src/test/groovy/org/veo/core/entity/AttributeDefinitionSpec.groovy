/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Jonas Jordan
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

import org.veo.core.entity.definitions.attribute.BooleanAttributeDefinition
import org.veo.core.entity.definitions.attribute.DateAttributeDefinition
import org.veo.core.entity.definitions.attribute.DateTimeAttributeDefinition
import org.veo.core.entity.definitions.attribute.DurationAttributeDefinition
import org.veo.core.entity.definitions.attribute.EnumAttributeDefinition
import org.veo.core.entity.definitions.attribute.ExternalDocumentAttributeDefinition
import org.veo.core.entity.definitions.attribute.IntegerAttributeDefinition
import org.veo.core.entity.definitions.attribute.ListAttributeDefinition
import org.veo.core.entity.definitions.attribute.TextAttributeDefinition
import org.veo.core.entity.exception.UnprocessableDataException

import spock.lang.Specification

class AttributeDefinitionSpec extends Specification{
    def "boolean attributes are validated"() {
        given:
        def definition = new BooleanAttributeDefinition()

        when: "validating booleans"
        definition.validate(true)
        definition.validate(false)

        then:
        noExceptionThrown()

        when: "validating an integer"
        definition.validate(5)

        then:
        thrown(UnprocessableDataException)
    }

    def "date attributes are validated"() {
        given:
        def definition = new DateAttributeDefinition()

        when: "validating dates"
        definition.validate("2020-01-01")
        definition.validate("2023-01-20")
        definition.validate("1969-12-31")

        then:
        noExceptionThrown()

        when: "validating a date in a bad format"
        definition.validate("2023-10-5")

        then:
        thrown(UnprocessableDataException)

        when: "validating a date in a non-existing month"
        definition.validate("2023-20-01")

        then:
        thrown(UnprocessableDataException)

        when: "validating a date on a non-existing day"
        definition.validate("2023-02-29")

        then:
        thrown(UnprocessableDataException)

        when: "validating a date time"
        definition.validate("2023-01-26T13:50:47+01:00")

        then:
        thrown(UnprocessableDataException)
    }

    def "date time attributes are validated"() {
        given:
        def definition = new DateTimeAttributeDefinition()

        when: "validating date times"
        definition.validate("2023-01-26T13:50:47+01:00")
        definition.validate("2023-01-26T13:50:47Z")

        then:
        noExceptionThrown()

        when: "validating a date time in a bad format"
        definition.validate("2023-1-26T13:50:47+01:00")

        then:
        thrown(UnprocessableDataException)

        when: "validating a date time in a non-existing month"
        definition.validate("2023-13-26T13:50:47+01:00")

        then:
        thrown(UnprocessableDataException)

        when: "validating a date time without a timezone"
        definition.validate("2023-01-26T13:50:47")

        then:
        thrown(UnprocessableDataException)
    }

    def "enum attributes are validated"() {
        given:
        def definition = new EnumAttributeDefinition(["red", "green", "blue"])

        when: "validating valid strings"
        definition.validate("red")
        definition.validate("green")
        definition.validate("blue")

        then:
        noExceptionThrown()

        when: "validating an invalid string"
        definition.validate("yellow")

        then:
        thrown(UnprocessableDataException)

        when: "validating an int"
        definition.validate(1)

        then:
        thrown(UnprocessableDataException)
    }

    def "external document attributes are validated"() {
        given:
        def definition = new ExternalDocumentAttributeDefinition()

        when: "validating valid URLs"
        definition.validate("http://test.example/doc.html")
        definition.validate("https://test.example/doc.html")
        definition.validate("ftp://test.example/doc.html")
        definition.validate("ftps://test.example/doc.html")
        definition.validate("smb://test.example/doc.html")

        then:
        noExceptionThrown()

        when: "validating a URL with an invalid char"
        definition.validate("https://test.example/doc html")

        then:
        thrown(UnprocessableDataException)

        when: "validating a URL with an invalid protocol"
        definition.validate("about://test.example/doc.html")

        then:
        thrown(UnprocessableDataException)

        when: "validating a URL with an another invalid protocol"
        definition.validate("file:///tmp/some-file")

        then:
        thrown(UnprocessableDataException)

        when: "validating a URL with no protocol"
        definition.validate("test.example/https://test.example/doc.html")

        then:
        thrown(UnprocessableDataException)

        when: "validating an int"
        definition.validate(5)

        then:
        thrown(UnprocessableDataException)
    }

    def "integer attributes are validated"() {
        given:
        def definition = new IntegerAttributeDefinition()

        when: "validating integers"
        definition.validate(42)
        definition.validate(-5)

        then:
        noExceptionThrown()

        when: "validating a string"
        definition.validate("42")

        then:
        thrown(UnprocessableDataException)

        when: "validating a float"
        definition.validate(42.0f)

        then:
        thrown(UnprocessableDataException)
    }

    def "list attributes are validated"() {
        given:
        def definition = new ListAttributeDefinition(new IntegerAttributeDefinition())

        when: "validating valid lists"
        definition.validate([])
        definition.validate([42])
        definition.validate([1, 3, 4, 6])

        then:
        noExceptionThrown()

        when: "validating a list with an invalid item"
        definition.validate([1, 2, 3, '4'])

        then:
        thrown(UnprocessableDataException)

        when: "validating an int"
        definition.validate(1)

        then:
        thrown(UnprocessableDataException)
    }

    def "text attributes are validated"() {
        given:
        def definition = new TextAttributeDefinition()

        when: "validating strings"
        definition.validate("T3XT M3")
        definition.validate("PL3453")
        definition.validate("!!!!!!!!1111111")

        then:
        noExceptionThrown()

        when: "validating an integer"
        definition.validate(5)

        then:
        thrown(UnprocessableDataException)
    }

    def "#value is a valid duration attribute"(value) {
        given:
        def definition = new DurationAttributeDefinition()

        when: "validating strings"
        definition.validate(value)

        then:
        noExceptionThrown()

        where:
        value << [
            'P1Y',
            'P7Y2M19D',
            'PT1H',
            'PT60M',
            'P7M3DT12H10M5S'
        ]
    }

    def "invalid duration attributes do not pass validation"() {
        given:
        def definition = new DurationAttributeDefinition()

        when: "validating strings"
        definition.validate(value)

        then:
        thrown(UnprocessableDataException)

        where:
        value << [
            'abc',
            'P1D2D',
            'P1H',
            'P',
            'T',
            'PT',
            'PT1HT',
            'P2B',
            'PT9Y',
            'PT3G',
            5
        ]
    }
}
