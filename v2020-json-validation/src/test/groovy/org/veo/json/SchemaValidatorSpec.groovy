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
 * Contributors:
 *     Alexander Ben Nasrallah <an@sernet.de> - initial API and implementation
 ******************************************************************************/
package org.veo.json

import com.fasterxml.jackson.databind.JsonNode
import spock.lang.Specification

class SchemaValidatorSpec extends Specification {

    private final static SchemaValidator validator = new SchemaValidator()

    def "validate"() {
        given:
        InputStream inputStream = this.class.getClassLoader().getResourceAsStream("order.json")
        when:
        ValidationResult result = validator.validate(inputStream)
        then:
        result.isSuccessful()
    }

    def "validate invalid additional property names"() {
        given:
        InputStream inputStream = this.class.getClassLoader().getResourceAsStream("invalid-property-name.json")
        when:
        ValidationResult result = validator.validate(inputStream)
        then:
        !result.successful
        when:
        JsonNode errorMessage = result.getMessages().find { message -> message.get('level').textValue() == 'error' }
        then:
        errorMessage.get('keyword').textValue() == 'additionalProperties'
        when:
        def unwanted = errorMessage.get('unwanted').collect{ it.textValue() }
        then:
        unwanted  == ['NoUpperCaseAtStart', 'no-DashAllowed', 'no_underScoreAllowed']
    }

    def "Validate invalid standard properties, i.e. id, type and title"() {
        given:
        InputStream inputStream = this.class.getClassLoader().getResourceAsStream("invalid-standard-properties.json")
        when:
        ValidationResult result = validator.validate(inputStream)
        then:
        !result.isSuccessful()
        JsonNode errorMessage = result.getMessages().find { message -> message.get('level').textValue() == 'error' }
        errorMessage.get('instance').get('pointer').textValue() == '/properties/$veo.id/type'
        errorMessage.get('keyword').textValue() == 'enum'
        errorMessage.get('value').textValue() == 'boolean'
    }

    def "Validate mixed predefined types"() {
        given:
        InputStream inputStream = this.class.getClassLoader().getResourceAsStream("mixed-predefined.json")
        when:
        ValidationResult result = validator.validate(inputStream)
        then:
        !result.isSuccessful()
        JsonNode errorMessage = result.getMessages().find { message -> message.get('level').textValue() == 'error' }
        errorMessage.get('instance').get('pointer').textValue() == '/properties/transportation'
        errorMessage.get('matched').intValue() == 0
    }

    def "Validate mixed subset types"() {
        given:
        InputStream inputStream = this.class.getClassLoader().getResourceAsStream("mixed-subset.json")
        when:
        ValidationResult result = validator.validate(inputStream)
        then:
        !result.isSuccessful()
        JsonNode errorMessage = result.getMessages().find { message -> message.get('level').textValue() == 'error' }
        errorMessage.get('instance').get('pointer').textValue() == '/properties/ingredients'
        errorMessage.get('matched').intValue() == 0
    }

    def "Validate predefined integers"() {
        given:
        InputStream inputStream = this.class.getClassLoader().getResourceAsStream("predefined-integers.json")
        when:
        ValidationResult result = validator.validate(inputStream)
        then:
        result.isSuccessful()
    }

    def "Validate subset of integers"() {
        given:
        InputStream inputStream = this.class.getClassLoader().getResourceAsStream("subset-integers.json")
        when:
        ValidationResult result = validator.validate(inputStream)
        then:
        result.isSuccessful()
    }
}
