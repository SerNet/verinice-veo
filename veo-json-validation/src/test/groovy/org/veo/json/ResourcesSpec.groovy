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
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.fge.jsonschema.core.report.ProcessingReport
import com.github.fge.jsonschema.main.JsonSchema
import com.github.fge.jsonschema.main.JsonSchemaFactory
import spock.lang.Specification

class ResourcesSpec extends Specification {

    private final static SchemaValidator validator = new SchemaValidator()

    def "validate the meta schema against draft-4"() {
        given:
        ObjectMapper mapper = new ObjectMapper()

        InputStream draft4Stream = this.class.getClassLoader().getResourceAsStream("json-schema-draft-4")
        JsonNode draft4Node = mapper.readTree(draft4Stream)
        JsonSchemaFactory factory = JsonSchemaFactory.byDefault()
        JsonSchema draft4 = factory.getJsonSchema(draft4Node)

        InputStream metaSchemaStream = Resources.getMetaSchemaAsStream()
        JsonNode metaSchemaNode = mapper.readTree(metaSchemaStream)

        when:
        ProcessingReport result = draft4.validate(metaSchemaNode)
        then:
        result.isSuccess()
    }
}
