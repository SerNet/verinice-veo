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
 *
 ******************************************************************************/
package org.veo.rest

import org.veo.json.SchemaValidator

import spock.lang.Specification
import spock.lang.Unroll

class SchemaValidationSpec extends Specification {

    def validator = new SchemaValidator()

    @Unroll
    def "Schema #schemaName validates against the meta-schema"() {
        given:
        def result = validator.validate(schemaStream)
        expect:
        result.successful
        where:
        schemaFile << new File('src/main/resources/schemas').listFiles()
        schemaStream = schemaFile.newInputStream()
        schemaName = schemaFile.name
    }
}
