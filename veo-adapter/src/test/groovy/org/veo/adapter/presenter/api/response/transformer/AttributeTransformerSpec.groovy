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
package org.veo.adapter.presenter.api.response.transformer

import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

import com.fasterxml.jackson.databind.ObjectMapper

import org.veo.core.entity.CustomProperties

import io.swagger.v3.core.util.Json
import spock.lang.Specification

class AttributeTransformerSpec extends Specification {

    AttributeTransformer sut = new AttributeTransformer()
    CustomProperties entity = Mock()
    ObjectMapper om = Json.mapper()

    def "applies string"() {
        given: "a string schema"
        def schema = om.valueToTree([
            "type" : "string"
        ])
        when: "a string is passed"
        sut.applyToEntity("vorname", "Max", schema, entity)
        then: "it is set"
        1 * entity.setProperty("vorname", "Max")
        when: "an integer is passed"
        sut.applyToEntity("vorname", 5, schema, entity)
        then: "an exception is thrown"
        thrown(IllegalArgumentException)
    }

    def "applies date"() {
        given: "a date schema"
        def schema = om.valueToTree([
            "type": "string",
            "format": "date-time"
        ])
        when: "a valid date string is passed"
        sut.applyToEntity("updated", "2020-02-02T00:00:00Z", schema, entity)
        then: "it is set"
        1 * entity.setProperty("updated", OffsetDateTime.parse("2020-02-02T00:00:00Z"))
        when: "an invalid date string is passed"
        sut.applyToEntity("updated", "2020-13-32T00:00:00Z", schema, entity)
        then: "an exception is thrown"
        thrown(DateTimeParseException)
    }

    def "applies boolean"() {
        given: "a boolean schema"
        def schema = om.valueToTree([
            "type": "boolean"
        ])
        when: "a boolean is passed"
        sut.applyToEntity("fachkunde", true, schema, entity)
        then: "it is set"
        1 * entity.setProperty("fachkunde", true)
        when: "a string is passed"
        sut.applyToEntity("fachkunde", "truthy", schema, entity)
        then: "an exception is thrown"
        thrown(IllegalArgumentException)
    }

    def "applies number"() {
        given: "a number schema"
        def schema = om.valueToTree([
            "type": "number"
        ])
        when: "an integer is passed"
        sut.applyToEntity("numberOfLegs", 8, schema, entity)
        then: "it is set as double"
        1 * entity.setProperty("numberOfLegs", 8d)
        when: "a float is passed"
        sut.applyToEntity("temperature", 8.36f, schema, entity)
        then: "it is set as double"
        1 * entity.setProperty("temperature", 8.36f.doubleValue())
        when: "a double is passed"
        sut.applyToEntity("angle", 10.4d, schema, entity)
        then: "it is set as double"
        1 * entity.setProperty("angle", 10.4d)
        when: "a string is passed"
        sut.applyToEntity("numberOfLegs", "8", schema, entity)
        then: "an exception is thrown"
        thrown(IllegalArgumentException)
    }

    def "applies enum"() {
        given: "an enum schema"
        def schema = om.valueToTree([
            "enum": [
                "ausbildung0",
                "ausbildung1",
                "ausbildung2"
            ]
        ])
        when: "applying a valid enum value"
        sut.applyToEntity("ausbildung", "ausbildung0", schema, entity)
        then: "it is set"
        1 * entity.setProperty("ausbildung", "ausbildung0")
        when: "applying an invalid enum value"
        sut.applyToEntity("ausbildung", "ausbildung3", schema, entity)
        then: "an exception is thrown"
        thrown(IllegalArgumentException)
    }

    def "applies enum list"() {
        given: "an enum list schema"
        def schema = om.valueToTree([
            "type": "array",
            "items": [
                "enum": [
                    "rolle0",
                    "rolle1",
                    "rolle2"
                ]
            ]
        ])
        when: "applying a valid list"
        sut.applyToEntity("rollen", ["rolle0", "rolle2"], schema, entity)
        then: "it is set"
        1 * entity.setProperty("rollen", ["rolle0", "rolle2"])
        when: "applying an invalid list"
        sut.applyToEntity("rollen", ["rolle0", "rolle3"], schema, entity)
        then: "an exception is thrown"
        thrown(IllegalArgumentException)
    }
}
