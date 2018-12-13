/*******************************************************************************
 * Copyright (c) 2018 Alexander Ben Nasrallah.
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
 *
 ******************************************************************************/
package org.veo.rest

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper

import org.veo.json.JsonValidator

import spock.lang.Specification
import spock.lang.Unroll

class ExamplesValidationSpec extends Specification {

    def validators = [
        'asset': new JsonValidator(this.class.getClassLoader().getResourceAsStream("schemas/elements/asset.json")),
        'person': new JsonValidator(this.class.getClassLoader().getResourceAsStream("schemas/elements/person.json")),
        'scenario': new JsonValidator(this.class.getClassLoader().getResourceAsStream("schemas/elements/scenario.json")),
        'control': new JsonValidator(this.class.getClassLoader().getResourceAsStream("schemas/elements/control.json"))
    ]

    @Unroll
    def "Example #exampleName validates against it's schema"() {
        given:
        JsonNode json = new ObjectMapper().readTree(exampleStream)
        def type = json.get('$veo.type').textValue()
        def result = validators[type].validate(json)
        expect:
        result.successful
        where:
        exampleFile << new File('src/test/resources/examples').listFiles()
        exampleStream = exampleFile.newInputStream()
        exampleName = exampleFile.name
    }
}
